const fs = require('fs');
const path = require('path');

// Target characters that might be corrupted
const targetChars = 'áéíóúÁÉÍÓÚñÑçÇãõÃÕâêîôûÂÊÎÔÛäëïöüÄËÏÖÜ¡¿§©şŞğĞıİèòùìàÈÒÙÌÀ';
const replacements = [];

// Node.js 'binary' is actually latin1 (ISO-8859-1).
// To simulate Windows-1252, we use a simple mapping for the 0x80-0x9F range.
const cp1252ToUnicode = new Map([
    [0x80, '\u20AC'], [0x82, '\u201A'], [0x83, '\u0192'], [0x84, '\u201E'],
    [0x85, '\u2026'], [0x86, '\u2020'], [0x87, '\u2021'], [0x88, '\u02C6'],
    [0x89, '\u2030'], [0x8A, '\u0160'], [0x8B, '\u2039'], [0x8C, '\u0152'],
    [0x8E, '\u017D'], [0x91, '\u2018'], [0x92, '\u2019'], [0x93, '\u201C'],
    [0x94, '\u201D'], [0x95, '\u2022'], [0x96, '\u2013'], [0x97, '\u2014'],
    [0x98, '\u02DC'], [0x99, '\u2122'], [0x9A, '\u0161'], [0x9B, '\u203A'],
    [0x9C, '\u0153'], [0x9E, '\u017E'], [0x9F, '\u0178']
]);

function decodeCp1252(buffer) {
    let result = '';
    for (let i = 0; i < buffer.length; i++) {
        const b = buffer[i];
        if (b >= 0x80 && b <= 0x9F) {
            result += cp1252ToUnicode.get(b) || String.fromCharCode(b);
        } else {
            result += String.fromCharCode(b);
        }
    }
    return result;
}

// Generate Single Corruptions
for (let i = 0; i < targetChars.length; i++) {
    const char = targetChars[i];
    const utf8Buffer = Buffer.from(char, 'utf8');
    const corrupted = decodeCp1252(utf8Buffer);
    if (corrupted !== char) {
        replacements.push([corrupted, char]);
        
        // Also generate double corruptions!
        const doubleUtf8Buffer = Buffer.from(corrupted, 'utf8');
        const doubleCorrupted = decodeCp1252(doubleUtf8Buffer);
        if (doubleCorrupted !== corrupted && doubleCorrupted !== char) {
            replacements.push([doubleCorrupted, char]);
        }
    }
}

// Sort replacements by length descending to replace double corruptions before single corruptions
replacements.sort((a, b) => b[0].length - a[0].length);

function walk(dir) {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach(function(file) {
        file = path.join(dir, file);
        const stat = fs.statSync(file);
        if (stat && stat.isDirectory()) {
            if (!file.includes('node_modules') && !file.includes('.git') && !file.includes('build')) {
                results = results.concat(walk(file));
            }
        } else {
            if (file.endsWith('.java') || file.endsWith('.yml')) {
                results.push(file);
            }
        }
    });
    return results;
}

const files = walk('.');
let fixedCount = 0;

files.forEach(file => {
    let content = fs.readFileSync(file, 'utf8');
    let changed = false;
    for (const [bad, good] of replacements) {
        if (content.includes(bad)) {
            content = content.split(bad).join(good);
            changed = true;
        }
    }
    if (changed) {
        fs.writeFileSync(file, content, 'utf8');
        console.log('Fixed ' + file);
        fixedCount++;
    }
});

console.log('Done! Fixed ' + fixedCount + ' files.');
