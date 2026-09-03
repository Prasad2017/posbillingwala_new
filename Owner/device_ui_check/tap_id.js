const fs = require('fs');
const xml = fs.readFileSync(process.argv[2], 'utf8');
const want = process.argv[3];
const re = new RegExp(
  'resource-id="[^"]*' + want + '"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"'
);
const m = xml.match(re);
if (!m) {
  const re2 = new RegExp('text="' + want + '"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"');
  const m2 = xml.match(re2);
  if (!m2) {
    console.error('not found', want);
    process.exit(1);
  }
  const x = (Number(m2[1]) + Number(m2[3])) / 2;
  const y = (Number(m2[2]) + Number(m2[4])) / 2;
  console.log(Math.round(x), Math.round(y));
  process.exit(0);
}
const x = (Number(m[1]) + Number(m[3])) / 2;
const y = (Number(m[2]) + Number(m[4])) / 2;
console.log(Math.round(x), Math.round(y));
