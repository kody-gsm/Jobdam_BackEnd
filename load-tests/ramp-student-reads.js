import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import exec from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'https://jobdom-fromt-git-test-vercel-preview-uen1.vercel.app/backend';
const token = __ENV.STUDENT_TOKEN;

const paths = [
  '/student/teachers',
  '/student/common',
  '/student/course',
  '/student/timetable?from=2026-10-02&to=2026-10-02',
  '/student/banner',
  '/recruit',
  '/form',
  '/schedules?from=2026-10-02&to=2026-10-02',
  '/api/notifications?page=0&size=20',
  '/api/notifications/unread-count',
];

export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '10s', target: 50 },
    { duration: '15s', target: 100 },
    { duration: '10s', target: 100 },
    { duration: '15s', target: 200 },
    { duration: '10s', target: 200 },
    { duration: '15s', target: 500 },
    { duration: '10s', target: 500 },
    { duration: '15s', target: 1000 },
    { duration: '10s', target: 1000 },
    { duration: '15s', target: 2000 },
    { duration: '10s', target: 2000 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    checks: [{ threshold: 'rate>0.99', abortOnFail: true, delayAbortEval: '5s' }],
    http_req_duration: [{ threshold: 'p(95)<2000', abortOnFail: true, delayAbortEval: '10s' }],
  },
};

function headers() {
  return { headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' } };
}

export function setup() {
  if (!token) fail('STUDENT_TOKEN 환경 변수가 필요합니다.');
  const response = http.get(`${baseUrl}/auth/profile`, headers());
  if (response.status !== 200) {
    fail(`토큰 확인 실패: GET /auth/profile returned ${response.status}`);
  }
}

export default function () {
  const path = paths[__ITER % paths.length];
  const response = http.get(`${baseUrl}${path}`, {
    ...headers(),
    timeout: '10s',
    tags: { endpoint: path.split('?')[0] },
  });
  const statusOk = check(response, {
    [`GET ${path.split('?')[0]} returns 2xx`]: (r) => r.status >= 200 && r.status < 300,
  });
  if (!statusOk) {
    exec.test.abort(`GET ${path.split('?')[0]} returned ${response.status}; stopping load test.`);
  }
  sleep(1);
}
