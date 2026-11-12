@echo off
rem Builds desktop\Squib.exe with the C# compiler that ships with Windows (.NET Framework 4).
rem Nothing to install. First desktop\port turns the phone app's code (app\src\main\java) and
rem squib.json into C# in desktop\gen, so the desktop app always matches the phone app.
setlocal
cd /d "%~dp0"
set FW=%WINDIR%\Microsoft.NET\Framework64\v4.0.30319
if not exist "%FW%\csc.exe" set FW=%WINDIR%\Microsoft.NET\Framework\v4.0.30319
if not exist build mkdir build

"%FW%\csc.exe" /nologo /out:build\port.exe /r:"%FW%\System.Web.Extensions.dll" port\Port.cs || goto fail
build\port.exe .. || goto fail

"%FW%\csc.exe" /nologo /target:winexe /optimize+ /nowarn:0108,0162,0168,0169,0219,0414,0649 /out:Squib.exe ^
  /r:"%FW%\WPF\PresentationFramework.dll" ^
  /r:"%FW%\WPF\PresentationCore.dll" ^
  /r:"%FW%\WPF\WindowsBase.dll" ^
  /r:"%FW%\System.Xaml.dll" ^
  /r:"%FW%\System.Drawing.dll" ^
  /r:"%FW%\System.Management.dll" ^
  /r:"%FW%\System.Web.Extensions.dll" ^
  /r:"%FW%\System.Windows.Forms.dll" ^
  /r:"%FW%\WPF\UIAutomationClient.dll" ^
  /r:"%FW%\WPF\UIAutomationTypes.dll" ^
  gen\*.cs src\*.cs || goto fail
echo built desktop\Squib.exe
exit /b 0

:fail
echo build failed
exit /b 1
