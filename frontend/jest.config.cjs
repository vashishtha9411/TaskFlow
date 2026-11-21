module.exports = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['@testing-library/jest-dom'],
  transform: { '^.+\\.tsx?$': '<rootDir>/jest.transform.cjs' },
};
