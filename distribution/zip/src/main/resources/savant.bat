@echo off

FOR /R %%F IN (savant-core-*.jar) DO (
  java -Xmx4G -jar %%F
  exit
)
