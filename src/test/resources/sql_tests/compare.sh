#!/bin/bash

# 设置目录路径
dir1="$(dirname "$0")/expected"
dir2="$(dirname "$0")/output"

# 设置颜色代码
green="\033[32m"
red="\033[31m"
yellow="\033[33m"
reset="\033[0m"

echo
echo "========================================================"
echo "         SQL File Comparison Tool - Expected vs. Actual"
echo "========================================================"
echo
echo "Expected directory: $dir1"
echo "Actual output directory: $dir2"
echo
echo "Starting file comparison..."
echo

total_files=0
matched_files=0
diff_files=0
missing_files=0

# 创建文件列表并避免管道导致的子shell问题
file_list=($(find "$dir1" -type f))

for file1 in "${file_list[@]}"; do
    # 获取相对路径
    relpath="${file1#$dir1/}"
    ((total_files++))

    # 检查目录2中是否存在相同的文件
    file2="$dir2/$relpath"
    if [ -f "$file2" ]; then
        # 比较文件内容
        if diff -q --strip-trailing-cr "$file1" "$file2" > /dev/null; then
            echo -e "${green}✓ MATCH${reset} - $relpath"
            ((matched_files++))
        else
            echo -e "${red}✗ DIFF${reset} - $relpath"
            ((diff_files++))
        fi
    else
        echo -e "${yellow}? MISSING${reset} - $relpath"
        ((missing_files++))
    fi
done

echo
echo "========================================================"
echo "                    Summary Report"
echo "========================================================"
echo "Total files: $total_files"
echo -e "${green}Matched files: $matched_files${reset}"
echo -e "${red}Different files: $diff_files${reset}"
echo -e "${yellow}Missing files: $missing_files${reset}"
echo "========================================================"

if [ $diff_files -gt 0 ]; then
    echo
    echo "Note: Found ${diff_files} files that don't match the expected results."
    echo "Please check the differences between actual and expected output."
fi

if [ $missing_files -gt 0 ]; then
    echo
    echo "Note: ${missing_files} expected files are missing in the output directory."
    echo "This might be due to processing errors or files not being generated."
fi