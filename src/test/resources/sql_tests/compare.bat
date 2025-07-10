@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul
rem 设置目录路径
set dir1=%~dp0expected
set dir2=%~dp0output

rem 启用ANSI颜色支持
for /F "tokens=1,2 delims=#" %%a in ('"prompt #$H#$E# & echo on & for %%b in (1) do rem"') do (
  set "ESC=%%b"
)

rem 设置颜色代码
set "green=%ESC%[32m"
set "red=%ESC%[31m"
set "yellow=%ESC%[33m"
set "reset=%ESC%[0m"

echo.
echo ========================================================
echo         SQL File Comparison Tool - Expected vs. Actual
echo ========================================================
echo.
echo Expected directory: %dir1%
echo Actual output directory: %dir2%
echo.
echo Starting file comparison...
echo.

set total_files=0
set matched_files=0
set diff_files=0
set missing_files=0

rem 获取目录1中的所有文件
for /r "%dir1%" %%f in (*) do (
    rem 获取相对路径
    set "relpath=%%f"
    set "relpath=!relpath:%dir1%\=!"
    set /a total_files+=1

    rem 检查目录2中是否存在相同的文件
    if exist "%dir2%\!relpath!" (
        rem 比较文件内容
        fc "%%f" "%dir2%\!relpath!" > nul
        if not errorlevel 1 (
            echo !green!✓ MATCH!reset! - !relpath!
            set /a matched_files+=1
        ) else (
            echo !red!✗ DIFF!reset! - !relpath!
            set /a diff_files+=1
        )
    ) else (
        echo !yellow!? MISSING!reset! - !relpath!
        set /a missing_files+=1
    )
)

echo.
echo ========================================================
echo                    Summary Report
echo ========================================================
echo Total files: %total_files%
echo !green!Matched files: %matched_files%!reset!
echo !red!Different files: %diff_files%!reset!
echo !yellow!Missing files: %missing_files%!reset!
echo ========================================================

if %diff_files% gtr 0 (
    echo.
    echo Note: Found %diff_files% files that don't match the expected results.
    echo Please check the differences between actual and expected output.
)

if %missing_files% gtr 0 (
    echo.
    echo Note: %missing_files% expected files are missing in the output directory.
    echo This might be due to processing errors or files not being generated.
)

endlocal