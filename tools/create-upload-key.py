#!/usr/bin/env python3
"""Create a local Google Play upload key once; never commit or package its secrets."""
import os
from pathlib import Path
import secrets
import shutil
import subprocess

root=Path(__file__).resolve().parent.parent
props=root/'signing.properties'
folder=root/'.signing'
key=folder/'5igna1-upload.p12'
if props.exists() or key.exists():
    raise SystemExit('Existing signing material found; left unchanged.')
keytool=Path(os.environ['JAVA_HOME'])/'bin/keytool' if os.environ.get('JAVA_HOME') else shutil.which('keytool')
if not keytool:
    raise SystemExit('Set JAVA_HOME to a JDK 17 or 21 installation.')
folder.mkdir(mode=0o700,exist_ok=True)
password=secrets.token_urlsafe(36)
env=dict(os.environ,FIVEIGNA1_UPLOAD_PASSWORD=password)
subprocess.run([str(keytool),'-genkeypair','-keystore',str(key),'-storetype','PKCS12','-alias','upload',
 '-keyalg','RSA','-keysize','4096','-sigalg','SHA256withRSA','-validity','10000',
 '-dname','CN=5igna1 Upload, OU=Android, O=Bongorian',
 '-storepass:env','FIVEIGNA1_UPLOAD_PASSWORD','-keypass:env','FIVEIGNA1_UPLOAD_PASSWORD'],env=env,check=True)
os.chmod(key,0o600)
with os.fdopen(os.open(props,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600),'w') as out:
    out.write(f'storeFile=.signing/5igna1-upload.p12\nstorePassword={password}\nkeyAlias=upload\nkeyPassword={password}\n')
subprocess.run([str(keytool),'-exportcert','-rfc','-keystore',str(key),'-alias','upload',
 '-storepass:env','FIVEIGNA1_UPLOAD_PASSWORD','-file',str(folder/'upload-certificate.pem')],env=env,check=True)
print('Created .signing/5igna1-upload.p12 and signing.properties. Keep both in a secure backup; do not commit them.')
