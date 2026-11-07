#!/usr/bin/env node
/**
 * javac-wasm.mj — offline Java compiler driver.
 *
 * Wraps the teavm-javac build of OpenJDK's javac (compiled to WebAssembly)
 * so the simulation module can be compiled in environments where neither a
 * JDK compiler nor Maven Central is reachable.
 *
 * Usage:
 *   node scripts/javac-wasm.mjs -d <outDir> [-cp <dir[:dir...]>] <source files...>
 *
 * Output: standard .class files (major version 65 / Java 21) written under -d.
 * Diagnostics are printed to stderr; exit code is 0 on success, 1 on failure.
 *
 * NOTE: this driver is a verification aid for restricted environments. The
 * canonical build is Gradle (see build.gradle), which uses the platform javac.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const toolDir = process.env.JAVAC_WASM_DIR || path.join(here, "..", "..", "tools", "javac-wasm");

function parseArgs(argv) {
  const opts = { outDir: null, classpath: [], sources: [] };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "-d") opts.outDir = argv[++i];
    else if (a === "-cp" || a === "-classpath") opts.classpath.push(...argv[++i].split(path.delimiter));
    else if (a === "--release" || a === "-source" || a === "-target") i++; // javac-wasm emits Java 21 bytecode
    else if (a.startsWith("-")) throw new Error(`unknown option: ${a}`);
    else opts.sources.push(a);
  }
  if (!opts.outDir) throw new Error("missing -d <outDir>");
  if (opts.sources.length === 0) throw new Error("no source files given");
  return opts;
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const { load } = await import(path.join(toolDir, "compiler.wasm-runtime.js"));
  const lib = await load(new Uint8Array(fs.readFileSync(path.join(toolDir, "compiler.wasm"))));
  const compiler = lib.exports.createCompiler();
  compiler.setSdk(new Int8Array(fs.readFileSync(path.join(toolDir, "compile-classlib-teavm.bin"))));
  compiler.setTeaVMClasslib(new Int8Array(fs.readFileSync(path.join(toolDir, "runtime-classlib-teavm.bin"))));

  let errors = 0;
  compiler.onDiagnostic((d) => {
    if (d.severity !== "error" && d.severity !== "warning") return;
    const where = d.fileName ? `${d.fileName}:${d.lineNumber ?? "?"}: ` : "";
    const line = `${where}${d.severity}: ${d.message}`;
    if (d.severity === "error") errors++;
    console.error(line);
  });

  // Classpath: pre-compiled classes from earlier invocations (e.g. main -> test).
  let classFiles = 0;
  for (const entry of opts.classpath) {
    const walk = (dir) => {
      for (const name of fs.readdirSync(dir)) {
        const full = path.join(dir, name);
        if (fs.statSync(full).isDirectory()) walk(full);
        else if (name.endsWith(".class")) {
          const rel = path.relative(entry, full).split(path.sep).join("/");
          compiler.addClassFile(rel, new Int8Array(fs.readFileSync(full)));
          classFiles++;
        }
      }
    };
    if (fs.existsSync(entry)) walk(entry);
  }

  compiler.clearSourceFiles();
  compiler.clearOutputFiles();
  for (const src of opts.sources) {
    const content = fs.readFileSync(src, "utf8");
    // javac requires the file name to match the public class; keep the path as given
    // so package-private cross-file references resolve the way javac expects.
    compiler.addSourceFile(path.basename(src), content);
  }

  const ok = compiler.compile();
  if (!ok || errors > 0) {
    console.error(`javac-wasm: compilation failed (${errors} error(s))`);
    process.exit(1);
  }

  const outputs = Array.from(compiler.listOutputFiles());
  fs.mkdirSync(opts.outDir, { recursive: true });
  for (const out of outputs) {
    const data = Buffer.from(compiler.getOutputFile(out));
    const dest = path.join(opts.outDir, out);
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.writeFileSync(dest, data);
  }
  console.log(`javac-wasm: ${opts.sources.length} source file(s), ${classFiles} classpath entr(ies), ${outputs.length} class file(s) -> ${opts.outDir}`);
}

main().catch((err) => {
  console.error(`javac-wasm: ${err.message}`);
  process.exit(1);
});
