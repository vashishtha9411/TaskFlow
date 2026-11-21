const ts = require('typescript');

module.exports = {
  process(source, filename) {
    const result = ts.transpileModule(source, {
      fileName: filename,
      compilerOptions: {
        target: ts.ScriptTarget.ES2022,
        module: ts.ModuleKind.CommonJS,
        jsx: ts.JsxEmit.ReactJSX,
        esModuleInterop: true,
        inlineSourceMap: true,
      },
    });
    return { code: result.outputText };
  },
};
