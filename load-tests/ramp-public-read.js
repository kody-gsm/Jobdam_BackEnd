import http from 'k6/http';
import { check, sleep } from 'k6';

const expectedStatus = 400;
const baseUrl = __ENV.BASE_URL || 'https://jobdom-fromt-git-test-vercel-preview-uen1.vercel.app/backend';

export const options = {
  stages: [
    { duration: '10s', target: 1 },
    { duration: '15s', target: 5 },
    { duration: '15s', target: 10 },
    { duration: '15s', target: 20 },
    { duration: '15s', target: 35 },
    { duration: '15s', target: 50 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    checks: [{ threshold: 'rate>0.99', abortOnFail: true, delayAbortEval: '5s' }],
    http_req_duration: [{ threshold: 'p(95)<2000', abortOnFail: true, delayAbortEval: '10s' }],
  },
};

http.setResponseCallback(http.expectedStatuses(expectedStatus));

export default function () {
  const response = http.get(`${baseUrl}/api/notifications/subscribe?ticket=invalid-ticket`, {
    tags: { endpoint: 'public-invalid-subscribe-ticket' },
  });
  check(response, {
    'API returns expected invalid-ticket status': (r) => r.status === expectedStatus,
  });
  sleep(1);
}
