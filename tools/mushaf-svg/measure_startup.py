#!/usr/bin/env python3
"""Compare process-cold launch-to-first-window timing on the same explicit device."""
import argparse
import json
from pathlib import Path
import statistics
import subprocess

parser=argparse.ArgumentParser()
parser.add_argument('--serial',required=True)
parser.add_argument('--baseline',required=True)
parser.add_argument('--candidate',required=True)
parser.add_argument('--output',required=True)
args=parser.parse_args()
adb=['/workspace/.android-sdk/platform-tools/adb','-s',args.serial]
rows=[]
for label,apk in [('before',args.baseline),('after',args.candidate)]:
    installed=subprocess.check_output(adb+['install','-r','-d',apk],text=True)
    assert 'Success' in installed,installed
    print(label+' installed',flush=True)
    for trial in range(3):
        subprocess.run(adb+['shell','am','force-stop','com.ahmad.quran'],check=True)
        output=subprocess.check_output(adb+['shell','am','start','-W','-n','com.ahmad.quran/.ui.MainActivity'],text=True)
        data=dict(line.split(': ',1) for line in output.splitlines() if ': ' in line)
        assert data.get('Status')=='ok',output
        rows.append(dict(build=label,trial=trial+1,totalTimeMs=int(data['TotalTime']),waitTimeMs=int(data['WaitTime']),raw=output))
        print(label+' '+str(rows[-1]['totalTimeMs'])+' ms',flush=True)
summary={label:statistics.median(r['totalTimeMs'] for r in rows if r['build']==label) for label in ['before','after']}
Path(args.output).write_text(json.dumps(dict(serial=args.serial,deviceKind='Software-emulated API23, same AVD; retained database, process-cold activity launches; first window, not index metadata readiness',trials=rows,medianTotalTimeMs=summary,medianDifferenceMs=summary['after']-summary['before']),indent=2)+'\n')
