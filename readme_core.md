## 说明
本项目使用了antlr4进行开发，针对ORACLE中部分SQL改写为GaussDB支持的SQL。  
项目使用 Maven 构建(也支持离线构建)，依赖 JDK 11 (11.0.24) 或以上版本。  

## 已支持的改写规则
- 对于不对称多行注释 ，将中间多余的`/*`改成`*`
- 对于`table($1)`函数，替换为`(select * from unnest_table($1) column_value)`。注意array类型不支持使用`unnest_table`,openGauss也不支持，如需替换成`unnest`，修改**config.properties**中的`tablefunctionreplace`参数
- 对于NEW构造器，去掉`NEW`关键字，并检测构造函数是否带括号，如无括号则补上
- 去掉宏编译代码 **（非等价改写，必然执行IF内的命令，且ELSE里的也会执行，检测到此类语法时会打印warning，需人工确认是否符合预期）**
- select列表分隔符、 函数传参列表分隔符 、where in列表、from列表、order by列表、group by 列表 、insert into目标列的列表、values 列表 的全角逗号(uFF0C)转换为半角逗号
- 全角空格(u3000)转换为半角空格
- 关键字作为字段别名但没带`AS`时，自动补上`AS`，支持的关键字列表见`.\src\main\java\PlSqlParser.g4`:`non_reserved_keywords_pre12c`/`non_reserved_keywords_in_12c`/`non_reserved_keywords_in_18c`/`non_reserved_keywords_in_gaussdb`,支持case when 表达式的别名为关键字
- 移除重复的declare变量，保留最后一个
~~- 对于`lag`和`lead`函数，当第三个参数为数字,第一个参数没有除法时，第3个参数加`.0`；有除法时，第3个参数加 `/1`~~
- 对于`lag`和`lead`函数，当第三个参数为数字时,把第三个参数加上单引号
- 移除函数、过程参数中的`nocopy`
- `select unique($1)`转成`select distinct($1)`
- 改写 `a [not] member of b` 为 `[not](a=any(b))`
- 对于`having`子句在`group by`前面的，把`having`子句 放到`group by`子句后面
- 将`result_cache [relies_on]` 替换成`immutable`
- `deteministic` 换成 `immutable`
- 按名称传参时，`=>`紧跟注释或`+`或`-`时，`=>`后面加空格
- 处理异常名称替换,需维护配置文件**exception_mapping.properties**，支持配置自定义异常,已预置`invalid_number`、`value_error`、`dup_val_on_index`
- 替换`raise_application_error`为`report_application_error`,并交换两个参数的顺序 **(`report_application_error`在openGauss被屏蔽使用)**
- 支持函数调用的替换，需维护配置文件**general_element.propertes**,已预置`SYS_CONTEXT('USERENV','SESSIONID')`->`pg_current_sessid()`,`SYS_CONTEXT('USERENV','IP_ADDRESS')`->`inet_out(inet_client_addr())`,`SYS_CONTEXT('USERENV','INSTANCE_NAME')`->`current_setting('pgxc_node_name')` 
- 对于`join (tablename)` 的用法，把表名两边的括号换成空格
- `.getclobval()` 和`.getclobval` 替换成 `::clob`
- 移除`xmlparse`函数内的`wellformed`（非等价改写，如果字段里有xml保留符号会报错）
- 支持识别`merge into ... using table() on`进行改写
- 支持基于token的全词替换，比如可配置高级包和系统视图的名称替换，配置文件为**token_mappig.properties** **（不考虑语法，仅考虑词法，配置需慎重）**
- 将 call_statement 中的 `dbms_obfuscation_toolkit.md5(input=> v1,checksum=>v2)`替换成 `v2:=hextoraw(md5(dbe_raw.cast_to_varchar2(v1)))`
- 将 expression 中的 `dbms_obfuscation_toolkit.md5(input=>v1)`替换成 `hextoraw(md5(dbe_raw.cast_to_varchar2(v1)))`
- 支持自定义正则表达式替换，在**patterns.yaml**文件中配置规则, **该规则先于其他所有规则执行**
- `grouping_id`函数 改成`grouping`
- to_date() -to_date()  改成 intervaltonum(to_date()-to_date())
- 目标库保留关键字作为字段名或变量名，支持配置映射文件修改，参数文件**regular_id_mapping.properties**,针对所有`regular_id`节点,已配置`authid`
- 表名替换（含系统视图名称），参数文件**regular_id_mapping.properties**
- 支持自定义数据类型映射，包括函数参数类型，函数返回类型，包、函数、匿名块声明的变量、常量类型，is table of的类型， index by的类型，cast函数内的类型，参数文件**datatype_mapping.properties**,预置了`pls_integer`、`char`
- 处理数据字典(user_col_comments/user_tables/user_objects/user_types/all_tab_columns/all_objects)名称替换参数文件**regular_id_mapping.properties**，
- 指定字段加函数进行转换（比如数据字典字段转大写）参数文件**atom_mapping.properties**，
- 如果要转换`owner`字段名为别的字段名或表达式，配置**config.properties**中的`ownerconvert`参数为期望替换成为的值（注意gaussdb的兼容系统视图可能会少列）
- 处理for update of [tablename.|aliasname.]{column_name|aliasname}，如果有表名，替换成表名；如果没有表名，从select列表中找一个匹配的;如果from后只有一个表，则取这个表名 ,支持多个字段的转换
- 去掉声明游标和打开游标中select into（实际ORACLE中并不会执行游标中的into，去掉的时候会弹个warning）
- 支持指定的查询语句进行语句替换,参数文件**query_block_mapping.properties**,key需要去掉所有空格和换行，`=`号需要转义，value需要转义特殊符号（包括空格和换行）
- 重复的表别名,暂无法自动改写，识别到会弹出警告`Duplicate table aliases found`，并且输出该查询块的hashCode值及该查询块的原始文本，可结合基于hashCode替换的功能进行自定义的整段替换 **建议辅助人工改造**
- 支持基于查询块hashCode值的整段替换，替换后的SQL以hashCode的值作为文件名放在config/query_block_hash_map目录下
- regexp_substr函数中的第二个参数中如果包含 `{}` 且没有转义，且里面的格式不满足`m`,`m,`,`m,n`，则对`{}`进行转义,同时会输出warning信息。
- 支持识别并转换全角括号成半角括号
- 支持指定输入和输出的字符集
- replace函数指定3个参数名称传参的，改成按定义的参数顺序传递，去掉参数名称
- 对于存在于`udtlist.properties`中的自定义类型，修改select语句中`type_name(c1,...)`形式的表达式为`cast((c1,...) as type_name)`
- 对于两表`left join`或`right join`时，连接条件带了多余的`(+)`的，去掉`(+)`
- 支持对于`execute immediate`里的动态SQL指定正则替换或移除规则，规则文件`dynamic_sql.yaml`
- q转义的字符串改写成不需要q转义
- 支持对于声明的char/varchar2变量或常量默认值为常量字符串时，检测声明的长度是否足够，如果不足则增加到默认值的字节长度（长度计算基于输出目标的字符集）
- update/insert/delete子句中的return替换成returning

## 构建和测试
### mvn构建项目
```bash
# mvn clean generate-sources # 生成解析器 修改g4文件后需再次执行）
# 全量构建且自动测试
mvn clean package

# 快速构建跳过测试
mvn package -DskipTests
```

### 离线构建(不需要mvn，但需要根据lib/download_url.txt下载依赖库放到lib目录下)
#### windows

```bash
# regen_parser.bat #生成解析器（修改g4文件后需再次执行）
build_offline.bat
```
#### Linux (or git-bash)

```bash
# ./regen_parser.sh #生成解析器（修改g4文件后需再次执行）
./build_offline.sh
```

### 运行测试
```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=com.plsqlrewriter.core.PlSqlRewriterTest#testBasicConversion

# windows手动测试 
## 单文件
convert.bat src\test\resources\sql_tests\input\fulltest.sql src\test\resources\sql_tests\output\fulltest.sql
## 批量
batch_convert.bat src\test\resources\sql_tests\input src\test\resources\sql_tests\output
## 比较output目录内所有文件是否和expected一致
src\test\resources\sql_tests\compare.bat 

# linux手动测试 
## 单文件
./convert.sh src/test/resources/sql_tests/input/fulltest.sql src/test/resources/sql_tests/output/fulltest.sql
## 批量
./batch_convert.sh src/test/resources/sql_tests/input src/test/resources/sql_tests/output
## 比较output目录内所有文件是否和expected一致
src/test/resources/sql_tests/compare.sh 
```

### 添加新的测试用例到mvn自动测试
在 `src/test/resources/sql_tests` 目录下的`input`和`expected`目录下创建配对的文件：
- `input/sample.sql`：输入文件
- `expected/sample.sql`：期望输出文件

## 使用方式
1. 单个文件转换：
```bash
# 使用默认 UTF-8 编码
java -cp target/plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar com.plsqlrewriter.core.PlSqlRewriter input.sql output.sql

# 指定编码
java -cp target/plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar com.plsqlrewriter.core.PlSqlRewriter input.sql output.sql GBK UTF-8

# 使用便捷脚本
./convert.sh input.sql output.sql GBK UTF-8  # Linux/Mac
convert.bat input.sql output.sql GBK UTF-8   # Windows
```

2. 批量转换：
```bash
# 使用默认 UTF-8 编码，10个并发
java -jar target/plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar com.plsqlrewriter.core.BatchProcessor input_dir output_dir 10

# 指定编码
java -jar target/plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar com.plsqlrewriter.core.BatchProcessor input_dir output_dir 10 GBK UTF-8

# 使用便捷脚本
./batch_convert.sh input_dir output_dir 10 GBK UTF-8  # Linux/Mac
batch_convert.bat input_dir output_dir 10 GBK UTF-8   # Windows
```

3. 查看语法树：
```bash
# Linux/Mac
./grun_tree.sh test.sql                 # 显示语法树
./grun_tree.sh -tokens test.sql         # 显示词法标记
./grun_tree.sh -gui test.sql            # 图形化显示语法树
./grun_tree.sh -encoding UTF-8 test.sql # 指定编码

# Windows
grun_tree.bat test.sql
grun_tree.bat -tokens test.sql
grun_tree.bat -gui test.sql
grun_tree.bat -encoding UTF-8 test.sql
```

## 项目目录结构
```
PlsqlRewrite4GaussDB
├─src
│  ├─main
│  │  ├─java
│  │  │  └─com
│  │  │      └─plsqlrewriter
│  │  │          ├─core
│  │  │          │  └─PlSqlRewriter.java   # 主程序源码
│  │  │          ├─parser
│  │  │          │  └─antlr
│  │  │          │      ├─PlSqlLexerBase.java  # 词法基类
│  │  │          │      └─PlSqlParserBase.java # 语法基类
│  │  │          └─util
│  │  │              ├─BatchProcessor.java  # 批量处理器
│  │  │              ├─PlSqlTreeViewer.java # 语法树查看器
│  │  │              └─CustomErrorListener.java # 定制错误监听器
│  │  └─resources
│  │      ├─antlr4                # ANTLR4语法文件目录
│  │      |   ├─PlSqlLexer.g4   # 词法解析规则
│  │      |   └─PlSqlParser.g4  # 语法解析规则
│  │      └─logback.xml # 日志配置
│  │
│  └─test
│      ├─java
│      │  └─com
│      │      └─plsqlrewriter
│      │          └─core
│      │              └─PlSqlRewriterTest.java  # 单元测试
│      │
│      └─resources
│          └─sql_tests              # 测试用例
│               ├─input                     # 测试输入文件
│               ├─output                    # 测试输出文件
│               ├─expected                  # 测试期望结果
│               ├─compare.bat              # Windows比较脚本
│               └─compare.sh               # Linux比较脚本
│
├─config                          # 配置文件目录
│  ├─query_block_hash_map        # 基于哈希值的查询块替换配置
│  ├─atom_mapping.properties      # 字段函数转换配置
│  ├─config.properties           # 通用配置
│  ├─datatype_mapping.properties # 数据类型映射
│  ├─exception_mapping.properties # 异常名称映射
│  ├─general_element_mapping.properties # 函数表达式映射
│  ├─patterns.yaml              # 正则替换规则
│  ├─patterns.properties        # 正则替换规则（旧版，已废弃）
│  ├─query_block_mapping.properties # 查询块规则
│  ├─regular_id_mapping.properties # 标识符映射
│  ├─token_mapping.properties    # 词法标记映射
│  └─udtlist.properties         # 自定义类型列表
│
├─lib                           # 离线构建依赖库
│
├─target                        # 编译输出目录
│  ├─classes                   # 编译后的类文件
│  ├─generated-sources         # 生成的源代码
│  │  └─antlr4                # ANTLR4生成的代码
│  ├─test-classes             # 编译后的测试类文件
│  └─*.jar                    # 打包的JAR文件
│
├─batch_convert.bat            # Windows批量转换脚本
├─batch_convert.sh            # Linux批量转换脚本
├─build_offline.bat           # Windows离线构建脚本
├─build_offline.sh           # Linux离线构建脚本
├─convert.bat                # Windows单文件转换脚本
├─convert.sh                # Linux单文件转换脚本
├─grun_tree.bat             # Windows语法树查看脚本
├─grun_tree.sh             # Linux语法树查看脚本
├─regen_parser.bat         # Windows重新生成解析器脚本
├─regen_parser.sh         # Linux重新生成解析器脚本
├─pom.xml                 # Maven项目配置
└─README.md               # 项目说明文档
```

## 配置文件说明
所有配置文件位于 `config` 目录下：

1. **atom_mapping.properties**: 指定字段使用函数转换
2. **config.properties**: 通用配置参数
3. **datatype_mapping.properties**: 数据类型映射关系
4. **exception_mapping.properties**: 异常名称映射
5. **general_element_mapping.properties**: 函数表达式映射
6. **patterns.yaml**: 正则表达式替换规则
7. **query_block_mapping.properties**: 查询块替换规则
8. **regular_id_mapping.properties**: 标识符映射关系
9. **token_mapping.properties**: 词法标记映射
10. **udtlist.properties**: 自定义类型列表

## 遗留问题
- SQLCODE数据类型不一致,暂无法自动处理，识别到就弹个warning，建议人工改造（可配置general_element_mapping，创建自定义函数转换成数值）
- dbms_utility的堆栈函数信息无法嵌套传递，暂无法自动处理，识别到就弹个warning，建议人工改造
- forall var.first .. var.last 改成 forall 1 .. var.count，原因是gaussdb的var.first在var未初始化时会报错 （当var是索引表类型时，该方式非等价改写，只能对不带index by的类型改写，建议人工改造）
- cursorname.columnname.objectattname ,多层引用需要改成通过变量中转（暂无法自动改写，也很难识别）
- select cast(mutilset as typename) into  val 改写成select bulk collect into (需要知道into变量类型的元素类型)

