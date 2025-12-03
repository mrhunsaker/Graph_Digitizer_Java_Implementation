import pathlib,re
root=pathlib.Path('src/main/java')
missing=[]
for p in sorted(root.rglob('*.java')):
    s=p.read_text(encoding='utf-8')
    m=re.search(r'^(?:\s*)public\s+(class|record|enum|interface)\s+\w+', s, flags=re.M)
    if m:
        start=m.start()
        before=s[:start]
        # look for a /** that starts a javadoc block in the last 10 lines before
        prev_lines=before.rstrip('\n').split('\n')[-10:]
        has_javadoc=False
        for line in reversed(prev_lines):
            if '/**' in line:
                has_javadoc=True
                break
            if line.strip()=='' or line.strip().startswith('*') or line.strip().startswith('/*'):
                continue
            if not line.strip().startswith('//'):
                break
        if not has_javadoc:
            missing.append(str(p))
print('\n'.join(missing))
print('MISSING_COUNT:', len(missing))
