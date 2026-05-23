import { resolveBackendOrigin } from './api.config';

describe('apiConfig', () => {
  it('uses the local backend while developing on localhost', () => {
    expect(resolveBackendOrigin('localhost', 'http://localhost:4200')).toBe(
      'http://localhost:8080',
    );
    expect(resolveBackendOrigin('127.0.0.1', 'http://127.0.0.1:4200')).toBe(
      'http://localhost:8080',
    );
  });

  it('uses the deployed client origin outside local development', () => {
    expect(resolveBackendOrigin('honest-car.example.com', 'https://honest-car.example.com')).toBe(
      'https://honest-car.example.com',
    );
  });
});
