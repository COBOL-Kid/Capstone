import { resolveBackendOrigin } from './api.config';

describe('apiConfig', () => {
  it('uses the page origin for API calls on localhost', () => {
    expect(resolveBackendOrigin('http://localhost:4200')).toBe('http://localhost:4200');
    expect(resolveBackendOrigin('http://127.0.0.1:4200')).toBe('http://127.0.0.1:4200');
  });

  it('uses the deployed client origin outside local development', () => {
    expect(resolveBackendOrigin('https://honest-car.example.com')).toBe(
      'https://honest-car.example.com',
    );
  });
});
