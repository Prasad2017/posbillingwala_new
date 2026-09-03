const fs = require('fs');
const path = process.argv[2];
const xml = fs.readFileSync(path, 'utf8');
const texts = [];
const click = [];
const re = /<node[^>]*>/g;
let m;
while ((m = re.exec(xml))) {
  const t = m[0];
  const get = (k) => {
    const x = t.match(new RegExp(k + '="([^"]*)"'));
    return x ? x[1] : '';
  };
  const text = get('text');
  const desc = get('content-desc');
  const bounds = get('bounds');
  const clickable = get('clickable');
  const rid = get('resource-id');
  const label = (text || desc).trim();
  if (label) texts.push({ label: label.slice(0, 120), bounds, rid, clickable });
  if (clickable === 'true') click.push({ label: (label || rid).slice(0, 80), bounds, rid });
}
console.log('TEXTS', texts.length);
texts.forEach((x) => console.log(JSON.stringify(x)));
console.log('---CLICK---');
click.forEach((x) => console.log(JSON.stringify(x)));
