import http from 'k6/http';
import { check, fail } from 'k6';
import encoding from 'k6/encoding';

const baseUrl = __ENV.BASE_URL || 'https://jobdom-fromt-git-test-vercel-preview-uen1.vercel.app/backend';
const tokens = JSON.parse(__ENV.STUDENT_TOKENS || '[]');
const refreshTokens = JSON.parse(__ENV.REFRESH_TOKENS || '[]');
const testDate = __ENV.TEST_DATE;
const slotCandidates = ['1교시', '2교시', '3교시', '5교시', '6교시', '7교시', '8교시', '9교시'];

export const options = {
  scenarios: {
    student_api_coverage: {
      executor: 'per-vu-iterations',
      vus: Math.max(tokens.length, 1),
      iterations: 1,
      maxDuration: '10m',
    },
  },
};

function auth(token) {
  return { headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' } };
}

function jsonParams(token) {
  return { headers: { Authorization: `Bearer ${token}`, Accept: 'application/json', 'Content-Type': 'application/json' } };
}

function checked(name, response, statuses = [200]) {
  return check(response, { [`${name} status ${statuses.join('/')}`]: (r) => statuses.includes(r.status) });
}

function get(token, path, statuses = [200]) {
  const response = http.get(`${baseUrl}${path}`, auth(token));
  checked(`GET ${path.split('?')[0]}`, response, statuses);
  return response;
}

function postJson(token, path, body, statuses = [200]) {
  const response = http.post(`${baseUrl}${path}`, JSON.stringify(body), jsonParams(token));
  checked(`POST ${path}`, response, statuses);
  return response;
}

function patchJson(token, path, body = {}, statuses = [200]) {
  const response = http.patch(`${baseUrl}${path}`, JSON.stringify(body), jsonParams(token));
  checked(`PATCH ${path}`, response, statuses);
  return response;
}

function testReservations(token, commonTeacherId, courseTeacherId, vu) {
  if (!testDate) {
    check(false, { 'TEST_DATE is set for reservation create/cancel coverage': () => false });
    return;
  }

  const nonce = `${Date.now()}-${vu}`;
  const commonStatus = get(token, `/student/common/status?teacherId=${commonTeacherId}&date=${testDate}`);
  const commonUsed = new Set((commonStatus.json() || []).filter((x) => x.state === 'LOCKED' || x.state === 'RESERVED' || x.mine).map((x) => x.period));
  const commonPeriod = slotCandidates.find((period) => !commonUsed.has(period));
  if (commonPeriod) {
    const title = `load-test-common-${nonce}`;
    const created = postJson(token, '/student/common', {
      title,
      content: 'Temporary load-test reservation; will be cancelled immediately.',
      category: 'ETC',
      date: testDate,
      period: commonPeriod,
      teacherId: commonTeacherId,
    });
    if (created.status === 200) {
      const rows = get(token, '/student/common').json() || [];
      const row = rows.find((item) => item.date === testDate && item.period === commonPeriod && item.teacherId === commonTeacherId && item.title === title);
      check(row, { 'created common reservation is returned with an id': (x) => Boolean(x && x.id) });
      if (row && row.id) patchJson(token, `/student/common/cancel/${row.id}`, {}, [200, 204]);
    }
  } else {
    check(false, { 'common counseling has an available test slot': () => false });
  }

  const courseStatus = get(token, `/student/course/status?teacherId=${courseTeacherId}&date=${testDate}`);
  const courseUsed = new Set((courseStatus.json() || []).filter((x) => x.state === 'LOCKED' || x.state === 'RESERVED' || x.mine).map((x) => x.period));
  const coursePeriod = slotCandidates.find((period) => !courseUsed.has(period));
  if (coursePeriod) {
    const title = `load-test-course-${nonce}`;
    const created = postJson(token, '/student/course', {
      title,
      content: 'Temporary load-test reservation; will be cancelled immediately.',
      category: 'ETC',
      date: testDate,
      period: coursePeriod,
      teacherId: courseTeacherId,
    });
    if (created.status === 200) {
      const rows = get(token, '/student/course').json() || [];
      const row = rows.find((item) => item.date === testDate && item.period === coursePeriod && item.teacherId === courseTeacherId && item.title === title);
      check(row, { 'created course reservation is returned with an id': (x) => Boolean(x && x.id) });
      if (row && row.id) patchJson(token, `/student/course/cancel/${row.id}`, {}, [200, 204]);
    }
  } else {
    check(false, { 'course counseling has an available test slot': () => false });
  }
}

function testForm(token, formId) {
  if (!formId) return;
  const detail = get(token, `/form/${formId}`);
  const form = detail.json();
  if (!form || form.status !== 'PUBLISHED') {
    check(false, { 'an open published form is available for submission coverage': () => false });
    return;
  }

  const questions = form.questions || [];
  const answers = [];
  let uploadedFileId = null;
  for (const question of questions) {
    const answer = { questionId: question.id };
    if (question.type === 'FILE') {
      if (!uploadedFileId) {
        const upload = http.post(`${baseUrl}/student/form/${formId}/file`, {
          file: http.file('temporary load-test attachment', 'load-test.txt', 'text/plain'),
        }, { headers: { Authorization: `Bearer ${token}` } });
        checked('POST /student/form/{id}/file', upload);
        if (upload.status >= 200 && upload.status < 300) {
          uploadedFileId = upload.json('id');
          if (uploadedFileId) get(token, `/form/file/${uploadedFileId}`);
        }
      }
      answer.fileId = uploadedFileId;
    } else if (question.options && question.options.length > 0) {
      answer.optionIds = [question.options[0].id];
    } else if (question.type === 'NUMBER') {
      answer.textValue = '1';
    } else if (question.type === 'DATE') {
      answer.textValue = testDate || '2026-10-07';
    } else {
      answer.textValue = 'load-test response';
    }
    answers.push(answer);
  }

  const existing = get(token, `/student/form/${formId}/submission`, [200, 404]);
  let saved;
  if (existing.status === 200) {
    saved = patchJson(token, `/student/form/${formId}/submission`, { answers });
  } else {
    saved = postJson(token, `/student/form/${formId}/submission`, { answers });
  }
  if (saved.status >= 200 && saved.status < 300) get(token, `/student/form/${formId}/submission`);
}

export default function () {
  if (tokens.length === 0) fail('STUDENT_TOKENS is required.');
  const index = (__VU - 1) % tokens.length;
  const token = tokens[index];
  const refreshToken = refreshTokens[index];

  get(token, '/auth/profile');
  const teachers = get(token, '/student/teachers').json() || [];
  get(token, '/api/teachers');
  const commonTeachers = get(token, '/student/common/teachers').json() || [];
  const courseTeachers = get(token, '/student/course/teachers').json() || [];
  get(token, '/student/common');
  get(token, '/student/course');
  get(token, '/student/banner');
  get(token, `/student/timetable?from=${testDate}&to=${testDate}`);
  get(token, `/schedules?from=${testDate}&to=${testDate}`);
  const [year, month] = (testDate || '2026-10-07').split('-');
  get(token, `/schedules/monthly?year=${year}&month=${Number(month)}`);

  const forms = get(token, '/form').json() || [];
  const publishedForm = forms.find((item) => item.status === 'PUBLISHED');
  if (publishedForm) testForm(token, publishedForm.id);
  else check(false, { 'published form fixture is available': () => false });

  const recruitments = get(token, '/recruit').json() || [];
  if (recruitments.length > 0) get(token, `/recruit/${recruitments[0].id}`);

  const notificationsResponse = get(token, '/api/notifications?page=0&size=20');
  const notifications = notificationsResponse.json();
  get(token, '/api/notifications/unread-count');
  if (notifications && notifications.content && notifications.content.length > 0) {
    patchJson(token, `/api/notifications/${notifications.content[0].id}/read`);
  }
  patchJson(token, '/api/notifications/read-all', {}, [204]);
  const ticketResponse = postJson(token, '/api/notifications/subscribe-ticket', {});
  if (ticketResponse.status === 200) {
    const ticket = ticketResponse.json('ticket');
    const stream = http.get(`${baseUrl}/api/notifications/subscribe?ticket=${encodeURIComponent(ticket)}`, {
      timeout: '5s',
      responseType: 'none',
      headers: { Accept: 'text/event-stream' },
    });
    checked('GET /api/notifications/subscribe (SSE connect)', stream);
  }

  const png = encoding.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jK8sAAAAASUVORK5CYII=', 'std');
  const profileImage = http.patch(`${baseUrl}/auth/profile/image`, {
    image: http.file(png, 'load-test.png', 'image/png'),
  }, { headers: { Authorization: `Bearer ${token}` } });
  checked('PATCH /auth/profile/image', profileImage);
  get(token, '/auth/profile');

  if (commonTeachers.length > 0 && courseTeachers.length > 0) {
    testReservations(token, commonTeachers[0].id, courseTeachers[0].id, __VU);
  } else {
    check(false, { 'common and course teacher fixtures are available': () => false });
  }

  if (refreshToken) {
    const reissued = postJson(token, '/auth/reissue', { refreshToken });
    if (reissued.status === 200) {
      const newRefreshToken = reissued.json('refreshToken');
      postJson(token, '/auth/logout', { refreshToken: newRefreshToken }, [204]);
    }
  }
}
