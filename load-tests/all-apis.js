import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter } from 'k6/metrics';

export const options = {
  stages: [
    { duration: '10s', target: Number(__ENV.VUS || 5) },
    { duration: __ENV.DURATION || '30s', target: Number(__ENV.VUS || 5) },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8088';
const RUN_MUTATIONS = (__ENV.RUN_MUTATIONS || 'false').toLowerCase() === 'true';
const TODAY = __ENV.TEST_DATE || '2026-09-30';
const YEAR = TODAY.slice(0, 4);
const MONTH = String(Number(TODAY.slice(5, 7)));

const expectedFailures = new Counter('expected_auth_or_validation_failures');

const studentHeaders = authHeaders(__ENV.STUDENT_TOKEN);
const teacherHeaders = authHeaders(__ENV.TEACHER_TOKEN);
const adminHeaders = authHeaders(__ENV.ADMIN_TOKEN);
const anonymousHeaders = { headers: { 'Content-Type': 'application/json' } };

function authHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return { headers };
}

function mark(name, res, okStatuses) {
  const ok = check(res, {
    [`${name}: expected status`]: (r) => okStatuses.includes(r.status),
    [`${name}: under 1s`]: (r) => r.timings.duration < 1000,
  });

  if (!ok && [400, 401, 403, 404].includes(res.status)) {
    expectedFailures.add(1);
  }
}

function get(name, path, params, okStatuses = [200, 204, 401, 403, 404]) {
  mark(name, http.get(`${BASE_URL}${path}`, params), okStatuses);
}

function post(name, path, body, params, okStatuses = [200, 201, 204, 400, 401, 403, 404]) {
  mark(name, http.post(`${BASE_URL}${path}`, JSON.stringify(body), params), okStatuses);
}

function patch(name, path, body, params, okStatuses = [200, 204, 400, 401, 403, 404]) {
  mark(name, http.patch(`${BASE_URL}${path}`, JSON.stringify(body), params), okStatuses);
}

function del(name, path, params, okStatuses = [200, 204, 400, 401, 403, 404]) {
  mark(name, http.del(`${BASE_URL}${path}`, null, params), okStatuses);
}

export default function () {
  group('auth', () => {
    post('POST /auth/login', '/auth/login', {
      email: __ENV.LOGIN_EMAIL || 'sloadtest@gsm.hs.kr',
      password: __ENV.LOGIN_PASSWORD || 'invalid-password',
    }, anonymousHeaders);

    post('POST /auth/reissue', '/auth/reissue', {
      refreshToken: __ENV.REFRESH_TOKEN || 'invalid-refresh-token',
    }, anonymousHeaders);

    post('POST /auth/logout', '/auth/logout', {
      refreshToken: __ENV.REFRESH_TOKEN || 'invalid-refresh-token',
    }, anonymousHeaders);

    get('GET /auth/profile', '/auth/profile', authHeaders(__ENV.STUDENT_TOKEN), [200, 401]);

    if (RUN_MUTATIONS) {
      post('POST /auth/email/signup-code', '/auth/email/signup-code', {
        email: __ENV.SIGNUP_EMAIL || 'sloadtest@gsm.hs.kr',
      }, anonymousHeaders);

      post('POST /auth/email/password-reset-code', '/auth/email/password-reset-code', {
        email: __ENV.RESET_EMAIL || 'sloadtest@gsm.hs.kr',
      }, anonymousHeaders);
    }
  });

  group('student reads', () => {
    get('GET /student/teachers', '/student/teachers', studentHeaders);
    get('GET /api/teachers', '/api/teachers', studentHeaders);
    get('GET /student/common/teachers', '/student/common/teachers', studentHeaders);
    get('GET /student/course/teachers', '/student/course/teachers', studentHeaders);
    get('GET /student/common', '/student/common', studentHeaders);
    get('GET /student/course', '/student/course', studentHeaders);
    get('GET /student/common/status', `/student/common/status?teacherId=1&date=${TODAY}`, studentHeaders);
    get('GET /student/course/status', `/student/course/status?teacherId=1&date=${TODAY}`, studentHeaders);
    get('GET /student/timetable', `/student/timetable?from=${TODAY}&to=${TODAY}`, studentHeaders);
    get('GET /student/banner', '/student/banner', studentHeaders);
  });

  group('teacher reads', () => {
    get('GET /teacher/students', '/teacher/students', teacherHeaders);
    get('GET /teacher/common', '/teacher/common', teacherHeaders);
    get('GET /teacher/common/pending', '/teacher/common/pending', teacherHeaders);
    get('GET /teacher/common/status', `/teacher/common/status?teacherId=1&date=${TODAY}`, teacherHeaders);
    get('GET /teacher/common/lock/weekly', '/teacher/common/lock/weekly', teacherHeaders);
    get('GET /teacher/course', '/teacher/course', teacherHeaders);
    get('GET /teacher/course/pending', '/teacher/course/pending', teacherHeaders);
    get('GET /teacher/course/status', `/teacher/course/status?teacherId=1&date=${TODAY}`, teacherHeaders);
    get('GET /teacher/course/lock/weekly', '/teacher/course/lock/weekly', teacherHeaders);
    get('GET /teacher/recruit', '/teacher/recruit', teacherHeaders);
    get('GET /teacher/form', '/teacher/form', teacherHeaders);
    get('GET /teacher/form/{id}', '/teacher/form/1', teacherHeaders);
    get('GET /teacher/form/{id}/submission', '/teacher/form/1/submission', teacherHeaders);
    get('GET /teacher/form/{id}/submission/{submissionId}', '/teacher/form/1/submission/1', teacherHeaders);
    get('GET /teacher/banner', '/teacher/banner', teacherHeaders);
  });

  group('shared authenticated reads', () => {
    get('GET /recruit', '/recruit', studentHeaders);
    get('GET /recruit/{id}', '/recruit/1', studentHeaders);
    get('GET /form', '/form', studentHeaders);
    get('GET /form/{id}', '/form/1', studentHeaders);
    get('GET /form/file/{fileId}', '/form/file/1', studentHeaders);
    get('GET /schedules', `/schedules?from=${TODAY}&to=${TODAY}`, studentHeaders);
    get('GET /schedules/monthly', `/schedules/monthly?year=${YEAR}&month=${MONTH}`, studentHeaders);
  });

  group('notifications', () => {
    get('GET /api/notifications', '/api/notifications?page=0&size=20', studentHeaders);
    get('GET /api/notifications/unread-count', '/api/notifications/unread-count', studentHeaders);
    post('POST /api/notifications/subscribe-ticket', '/api/notifications/subscribe-ticket', {}, studentHeaders);
    get('GET /api/notifications/subscribe', '/api/notifications/subscribe?ticket=invalid-ticket', anonymousHeaders, [400, 401, 403, 404, 500]);
  });

  group('admin', () => {
    if (RUN_MUTATIONS) {
      post('POST /admin/students/sync', '/admin/students/sync', {}, adminHeaders);
    } else {
      post('POST /admin/students/sync auth check', '/admin/students/sync', {}, anonymousHeaders, [401, 403]);
    }
  });

  if (RUN_MUTATIONS) {
    group('mutations', () => {
      const reservation = {
        teacherId: Number(__ENV.TEACHER_ID || 1),
        date: TODAY,
        period: __ENV.PERIOD || '4교시',
        reason: 'k6 load test',
      };

      post('POST /student/common', '/student/common', reservation, studentHeaders);
      post('POST /student/course', '/student/course', reservation, studentHeaders);
      patch('PATCH /student/common/cancel/{reservation_id}', '/student/common/cancel/1', {}, studentHeaders);
      patch('PATCH /student/course/cancel/{reservation_id}', '/student/course/cancel/1', {}, studentHeaders);

      post('POST /teacher/common/force', '/teacher/common/force', reservation, teacherHeaders);
      post('POST /teacher/course/force', '/teacher/course/force', reservation, teacherHeaders);
      patch('PATCH /teacher/common/allow/{id}', '/teacher/common/allow/1', {}, teacherHeaders);
      patch('PATCH /teacher/common/reject/{id}', '/teacher/common/reject/1', {}, teacherHeaders);
      patch('PATCH /teacher/course/allow/{id}', '/teacher/course/allow/1', {}, teacherHeaders);
      patch('PATCH /teacher/course/reject/{id}', '/teacher/course/reject/1', {}, teacherHeaders);

      post('POST /teacher/common/lock', '/teacher/common/lock', reservation, teacherHeaders);
      post('POST /teacher/common/unlock', '/teacher/common/unlock', reservation, teacherHeaders);
      post('POST /teacher/course/lock', '/teacher/course/lock', reservation, teacherHeaders);
      post('POST /teacher/course/unlock', '/teacher/course/unlock', reservation, teacherHeaders);

      const weeklyLock = { dayOfWeek: 'WEDNESDAY', period: __ENV.PERIOD || '4교시' };
      post('POST /teacher/common/lock/weekly', '/teacher/common/lock/weekly', weeklyLock, teacherHeaders);
      post('POST /teacher/common/unlock/weekly', '/teacher/common/unlock/weekly', weeklyLock, teacherHeaders);
      post('POST /teacher/course/lock/weekly', '/teacher/course/lock/weekly', weeklyLock, teacherHeaders);
      post('POST /teacher/course/unlock/weekly', '/teacher/course/unlock/weekly', weeklyLock, teacherHeaders);

      post('POST /teacher/recruit', '/teacher/recruit', {
        companyName: 'k6-test',
        title: 'load test',
        content: 'load test',
      }, teacherHeaders);
      patch('PATCH /teacher/recruit/{id}', '/teacher/recruit/1', { title: 'load test updated' }, teacherHeaders);
      post('POST /teacher/recruit/{id}/publish', '/teacher/recruit/1/publish', {}, teacherHeaders);
      del('DELETE /teacher/recruit/{id}', '/teacher/recruit/1', teacherHeaders);

      const form = {
        title: 'k6 load test form',
        description: 'created by k6',
        questions: [{ title: 'question', type: 'TEXT', required: true }],
      };
      post('POST /teacher/form', '/teacher/form', form, teacherHeaders);
      patch('PATCH /teacher/form/{id}', '/teacher/form/1', form, teacherHeaders);
      post('POST /teacher/form/{id}/publish', '/teacher/form/1/publish', {}, teacherHeaders);
      post('POST /teacher/form/{id}/close', '/teacher/form/1/close', {}, teacherHeaders);
      post('POST /teacher/form/{id}/submission/{submissionId}/confirm', '/teacher/form/1/submission/1/confirm', {}, teacherHeaders);
      del('DELETE /teacher/form/{id}/submission/{submissionId}/confirm', '/teacher/form/1/submission/1/confirm', teacherHeaders);
      del('DELETE /teacher/form/{id}', '/teacher/form/1', teacherHeaders);

      post('POST /student/form/{id}/submission', '/student/form/1/submission', { answers: [] }, studentHeaders);
      patch('PATCH /student/form/{id}/submission', '/student/form/1/submission', { answers: [] }, studentHeaders);

      patch('PATCH /api/notifications/{notificationId}/read', '/api/notifications/1/read', {}, studentHeaders);
      patch('PATCH /api/notifications/read-all', '/api/notifications/read-all', {}, studentHeaders);

      post('POST /api/notices', '/api/notices', {
        title: 'k6 load test notice',
        content: 'load test',
        link: 'https://example.com',
      }, teacherHeaders);
      patch('PATCH /api/notices/{messageId}', '/api/notices/1', {
        title: 'k6 load test notice updated',
        content: 'load test',
        link: 'https://example.com',
      }, teacherHeaders);
    });
  }

  sleep(1);
}
