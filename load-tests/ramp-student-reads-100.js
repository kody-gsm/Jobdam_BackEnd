import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import exec from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'https://jobdam.kro.kr/backend';
const token = __ENV.STUDENT_TOKEN;
const testDate = __ENV.TEST_DATE;

const paths = [
  '/student/teachers',
  '/student/common',
  '/student/course',
  `/student/timetable?from=${testDate}&to=${testDate}`,
  '/student/banner',
  '/recruit',
  '/form',
  `/schedules?from=${testDate}&to=${testDate}`,
  '/api/notifications?page=0&size=20',
  '/api/notifications/unread-count',
];

export const options = {
  stages: [
    { duration: '20s', target: 10 },
    { duration: '20s', target: 10 },
    { duration: '20s', target: 25 },
    { duration: '20s', target: 25 },
    { duration: '20s', target: 50 },
    { duration: '30s', target: 50 },
    { duration: '20s', target: 75 },
    { duration: '30s', target: 75 },
    { duration: '20s', target: 100 },
    { duration: '60s', target: 100 },
    { duration: '20s', target: 0 },
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
  if (!token) fail('STUDENT_TOKEN environment variable is required.');
  if (!testDate) fail('TEST_DATE environment variable is required (YYYY-MM-DD).');
  const response = http.get(`${baseUrl}/auth/profile`, headers());
  if (response.status !== 200) fail(`Token check failed: /auth/profile returned ${response.status}.`);
}

export default function () {
  const path = paths[__ITER % paths.length];
  const response = http.get(`${baseUrl}${path}`, {
    ...headers(),
    timeout: '10s',
    tags: { endpoint: path.split('?')[0] },
  });
  const ok = check(response, {
    [`GET ${path.split('?')[0]} returns 2xx`]: (r) => r.status >= 200 && r.status < 300,
  });
  if (!ok) exec.test.abort(`GET ${path.split('?')[0]} returned ${response.status}; stopping.`);
  sleep(1);
}
