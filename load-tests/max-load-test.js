import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'https://jobdom-fromt-uen1.vercel.app/backend';

export const options = {
  stages: [
    { duration: '20s', target: 100 },
    { duration: '20s', target: 500 },
    { duration: '20s', target: 1000 },
    { duration: '20s', target: 2000 },
    { duration: '20s', target: 3000 },
    { duration: '20s', target: 5000 },
    { duration: '20s', target: 0 },
  ],

  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/recruit`, {
    timeout: '10s',
  });

  check(res, {
    'GET /recruit status 200': (r) => r.status === 200,
  });

  sleep(1);
}
