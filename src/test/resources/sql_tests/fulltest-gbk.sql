--处理new构造器
declare
l_ty_a ty_a:=new ty_a(null,null);
l_tyt_a tyt_a:=new tyt_a();
l_tyt_varchar tyt_varchar:=new tyt_varchar();
l_tyt_varchar2 tyt_varchar:=new
                                 tyt_varchar();
l_tyt_b tyt_a:=new tyt_a;
l_tyt_C tyt_a := tyt_a;
l_tyt_Cd tyt_a := tyt_a();

x int;
begin
l_tyt_varchar :=new tyt_varchar();
l_tyt_varchar :=new tyt_varchar;
l_tyt_varchar := tyt_varchar;
--处理 table()
select count(1) into x from table(l_tyt_varchar) t where t.column_value ='a';

select count(1) into x from table(l_tyt_a) t where t.a ='1';
select count(1) into x from table(l_tyt_varchar) t where t.column_value ='a';

select count(1) into x from table(l_tyt_a) t where t.a ='1';

select count(1) into x from t123 t,table(l_tyt_varchar) p where t.c1=p.column_value; 
select count(1) into x from t123 t where exists (select 1 from table(l_tyt_varchar) p where t.c1=p.column_value); 

select count(1) into x from t123 t where exists (select 1 from table(l_tyt_varchar) where t.c1=column_value); 

select count(1) into x from t123 t where exists (select 1 from table(l_tyt_a) p where t.c1=p.a); 

select count(1) into x from table123; 
end;
/


DECLARE
--l_ty_test ty_test:= ty_test();
l_ty_test2 ty_test2:= ty_test2();
l_ty_r_test ty_r_test:=new ty_r_test();
b varchar2(10);
c varchar2(20);
d varchar2(20);
e varchar2(20);
BEGIN
null;

end;
/

create or replace package pkg_test IS
procedure pro1(l_ty_test in out ty_test) ;
end;
/

create or replace package body pkg_test IS
procedure pro1(l_ty_test in out ty_test) IS
--l_ty_test ty_test:= ty_test();
l_ty_test2 ty_test2:= ty_test2();
l_ty_r_test ty_r_test:=new ty_r_test();
b varchar2(10);
c varchar2(20);
d varchar2(20);
e varchar2(20);
BEGIN
null;

end;
end;
/
--处理注释
select * /*/*/ from dual;
select * /*//*/ from dual;
select * /*/**/ from dual;
select * /**/ from dual;
select * /*111*/ from dual;
select * /*22/*2*/ from dual;
select * /*33
/*4*/ from dual;
select * --/*aaa*/
 from dual;
select * --/*a
/*aa*/ from dual;
select /* 1/2 */ 1 from dual;

--处理 条件编译 $IF
declare
l number;
begin
$IF $$ABC OR $$ABC IS NULL $THEN
L:=1;
UP_INSER(1,2);
$ELSE
RETURN;
$END
L:=2;
END;
/

--处理全角逗号和全角空格
select 1，2,3 from dual;
select func(a，b) from dual;
select func(i=>a，o=>b,io=>c)from dual;
select 1  　from dual;
select　　1　　from　　dual　　;　　
select 1，2,3 from dual，dual,dual where a in (1，2,3) group by 1，2,3 order by 1，2,3;
insert into tab(c1，c2,c3) values (1，2,3);

--处理全角括号
select * from t where （1=2 and 2=2） and （1=2 and 2=2）;

--处理关键字作为字段别名
select 1 as language from dual;
select 1  language from dual;
select 1 SPECIFICATION from dual;
select 1 OPERATOR from dual;
select 1 POSITION from dual;
select sum(1)  POSITION from dual;
select trim('aa')||trim('bb') position from dual;

SELECT 1 FROM (
SELECT DENSE_RANK() OVER(ORDER BY 1) DENSE_RANK FROM DUAL)
GROUP BY DENSE_RANK;
select case when 1=1 then 1 end DENSE_RANK from dual;

-- 字段别名带双引号不能报错
select 1 "a" , 2 "啊" from dual;

--处理重复声明的变量
declare
x int;
y varchar2(10);
x number;      
begin
null;
declare
x int;
y varchar2(10);
x number;      
begin
null;
end; 
end; 
/
declare
x int;
y varchar2(10);
x number;      
begin
null;
end; 
/
declare
x int;
y varchar2(10);
x int;      
begin
null;
end; 
/

create package body test_pkg is 
procedure a is
x int;
y varchar2(10);
x number;      
begin
 null;
 declare
  x int;
  y varchar2(10);
  x number;      
 begin
  null;
 end; 
end; 
procedure b is
y varchar2(10);
begin
null;
end; 
end;
/

--处理lag和lead
select lag(a,1,1) over(order by c) from t;  
select lag(a,1,1.1) over(order by c) from t;  
select lag(a,1,'1') over(order by c) from t;  
select lag(a,1) over(order by c) from t;  
select lag(a/b,1,1) over(order by c) from t;  
select lag(ceil(a/b),1,1) over(order by c) from t;  

select lead(a,1,1) over(order by c) from t;  
select lead(a,1,'1') over(order by c) from t;  
select lead(a,1) over(order by c) from t;  
select lead(a/b,1,1) over(order by c) from t;  
select lead(ceil(a/b),1,1) over(order by c) from t; 

--处理nocopy
create procedure t(a in out  nocopy number,
b in number) is
begin
null;
end;
/
select 1 as nocopy from dual;

--处理unique
select unique(a) from t;

--处理pls_integer
declare
type x is table of int index by pls_integer;
begin
  null;
  end;
/
select 1 as pls_integer from dual;
create function b(a pls_integer,a2 number) return pls_integer is
type x is table of int index by pls_integer;
c pls_integer;
begin
  return 1;
end;
/

--处理not member of
create type ty_a is object (b VARCHAR2(10));
/
CREATE TYPE TYT_a IS TABLE OF TY_A;
/
CREATE TYPE TYT_VARCHAR2 IS TABLE OF VARCHAR2(10);
/

declare
l_ty_a ty_a;
L_TYT_A TYT_a:=TYT_a();
L_TYT_VARCHAR2 TYT_VARCHAR2:=TYT_VARCHAR2();
L INT;
begin
  l_ty_a:=ty_a(1);
  L_TYT_A.EXTEND;
  L_TYT_A(1):=l_ty_a;
  L_TYT_VARCHAR2:=TYT_VARCHAR2('1');
if L_TYT_A(1).B not member of L_TYT_VARCHAR2 then 
  dbms_output.put_line('Y');
  else 
  dbms_output.put_line('N');
end if;
if L_TYT_A(1).B  member of L_TYT_VARCHAR2 then 
  dbms_output.put_line('Y');
  else 
  dbms_output.put_line('N');
end if;

L:=CASE WHEN L_TYT_A(1).B not member of L_TYT_VARCHAR2 THEN 1 END;
L:=CASE WHEN L_TYT_A(1).B  member of L_TYT_VARCHAR2 THEN 1 END;
end;
/

--处理having在group by前面
select count(1) from t having count(1)>1 group by id; 
select id,name,count(1) from t having count(1)>1 group by id,name; 
select id,name,count(1) from t where 1=1 having count(1)>sum(1) and count(1)<avg(1) group by id,name; 
select id,name,count(123) from t where 1=1 group by id,name having count(123)>sum(123) and count(123)<=avg(123); 
select count(1) from t having count(1)>1;
select count(1) from t group by a;
select count(1) from t 
    group by id
   having count(1)>1; 


--处理 xmlagg(xmlparse(content {expression} wellformed)).getclobval() ,移除getclobval和wellformed
select a,rtrim(xmlagg(xmlparse(content trim(b)||',' wellformed) order by b).getclobval(),',') from t;
select a,rtrim(xmlagg(xmlparse(content trim(b)||',' wellformed) order by b).getclobval,',') from t;
select a.getclobval() from dual;
DECLARE
x varchar2(10);
y xml;
BEGIN
x:=y.getclobval();
end;
/

--处理result_cache relies_on
create function uf return number 
result_cache
relies_on(t) IS
BEGIN
return 1;
end;
/
create function uf return number 
result_cache IS
BEGIN
return 1;
end;
/
create package pkgs_test_result IS
function uf return number result_cache ;
end;
/
CREATE PACKAGE BODY PKGS_TEST_RESULT IS
function uf return number result_cache relies_on(t) IS
BEGIN
return 1;
end;
END;
/

--处理deterministic
create function uf return number 
deterministic IS
BEGIN
return 1;
end;
/
create function uf return number 
IMMUTABLE IS
BEGIN
return 1;
end;
/
create package p IS
function uf return number deterministic;
end;
/
CREATE PACKAGE BODY P IS 
function uf return number 
deterministic IS
BEGIN
return 1;
end;
END;
/ 
--处理按名称传参紧接注释
select func(a=> /*b*/ 1) from dual;
select func(a=>/*b*/ 1) from dual;
select func(a=>--TEST
 1) from dual;

--处理=>- 和=>+
select func(a=>-1) from dual;
select func(a=>+1) from dual;

--处理异常名称映射（基于配置文件excption_mapping.properties）
begin
  null;
  exception when pkgs_e.lock_error then
    null;
end;
/
begin
  null;
  exception when invalid_number then
    null;
end;
/

--处理dml里的end case
select 1 case from dual;
select case when 1=1 then 1 end case from dual;
BEGIN
if 1=1 then
for i in 1..2 loop
case when 1=1 THEN
case when 1=1 THEN
null;
end case;
end case;
end loop;
end if;
end;
/

--处理raise_application_error
begin
raise_application_error(-20001,'a');
raise_application_error(-20001,'a'||'b');
raise_application_error(-20001,'a'||'b'||func('a','b'));
end;
/

--处理SYS_CONTEXT
select SYS_CONTEXT('USERENV','SESSIONID') col1,
SYS_CONTEXT( 'USERENV' , 'SESSIONID' ) col2,
SYS_CONTEXT('USERENV','IP_ADDRESS') col3,
SYS_CONTEXT('USERENV','INSTANCE_NAME') col4
 from dual;

declare
a varchar2(200);
begin
a:=SYS_CONTEXT('USERENV','SESSIONID');
a:=SYS_CONTEXT('USERENV', 'IP_ADDRESS' );
a:=SYS_CONTEXT ('USERENV','INSTANCE_NAME');
end;
/

--处理(tablename)
select 1 from t1 join (t2) on t1.id=t2.id;
select 1 from t1 join(t2) on t1.id=t2.id;
select 1 from t1 join t2 on t1.id=t2.id;
select 1 from t1 join (select id from t2) t3 on t1.id=t3.id; 
select 1 from t1 join (t2 join t4 on t2.id=t4.id) t3 on t1.id=t3.id; 
DECLARE
x int;
begin
select 1 
into x 
from table(v) t1 
LEFT JOIN (t_xxx) t2
on t1.id=t2.id;
end;
/
select 1 from table(v) t1 LEFT JOIN (t_xxx) t2 on t1.id=t2.id;
select 1 from table(v) t1 JOIN (t_xxx) t2 on t1.id=t2.id;

--处理using table()
merge into t1
using table(i_tyt) t2
on (t1.id =t2.id)
when matched then 
update set t1.name=t2.name;

merge into t1
using (select id,name from t2 ) t2
on (t1.id=t2.id)
when matched then 
update set t1.name=t2.name;

merge into t1
using t2
on (t1.id =t2.id)
when matched then 
update set t1.name=t2.name;

--处理高级包
DECLARE
x varchar2(1000);
y raw;
z varchar2(1000);
n raw;
BEGIN
dbms_output.put_line('a');
x:=dbms_utility.format_error_backtrace();
dbms_application_info.set_moudle('xxx','yyy');
dbms_obfuscation_toolkit.md5(input=>y,checksum=>n);
dbms_obfuscation_toolkit.md5(input_string=>x,checksum_string=>z); --不支持，无可映射函数
n:=dbms_obfuscation_toolkit.md5(input=>y);
x:=rawtohex(dbms_obfuscation_toolkit.md5(input=>y));
select dbms_obfuscation_toolkit.md5(input=>y) into n from dual;
z:=dbms_obfuscation_toolkit.md5(input_string=>x); --不支持，无可映射函数
end;
/

--处理正则替换
declare
v1 ty_test;
v2 ty_test2:= ty_test2();
v3 tyt_test:= tyt_test();
x text;
BEGIN
v1:=ty_test2(2,'b') ;
v3(1):=v1;
select ty_test2(1,'a') into v2 from dual ;
--v1.up_copytype(v2); --注释不要替换
v1.up_copytype(v2);--v1.up_copytype(v2);
v3(2).up_copytype(v3(1));
x:=v1.uf_tostring();
x:='a' || v1.uf_tostring();
--x:=v1.uf_tostring(9); --没有这个用法
v3.extend;
v3(2):=v2;

for i in 1..v3.count loop
x:=v3(i).uf_tostring();
X:=X||'ABC'||v3(i).uf_tostring();
pkgs_logmanager.up_Debug(l_var, 'i_ty:'||l_ty.uf_tostring());
pkgs_logmanager.up_Debug(l_var,l_ty.uf_tostring());

x:=substrb('xxxx'||
 i_tysystemenv.uf_tostring()
 ||',' ,1,4000);

 x:=substrb('xxxx'
 ||i_tysystemenv.uf_tostring()
 ||',' ,1,4000);

  x:=substrb('xxxx'
 ||i_tysystemenv.uf_tostring() ||','
 ||',' ,1,4000);
end loop;
end;
/

declare
v1 ty_test;
v2 ty_test2:= ty_test2();
v3 tyt_test:= tyt_test();
x text;
BEGIN
v1.up_copytype(v2);
v1.up_copytype(i_tyt => v3);
v3(2).up_copytype(v3(1));
v3(2).up_copytype(ityt =>v3(5));
v3(2).up_copytype(v4);
v3(2).up_copytype(v2341(iii));
x:=v1.uf_tostring();
for i in 1..v3.count loop
x:=v3(i).uf_tostring();
X:=X||'ABC'||v3(i).uf_tostring();
end loop;
end;
/

declare
x number;
BEGIN
$IF $$ABC OR $$ABC IS NULL $THEN
 O_NRETCODE := SPF.PKGF_SPHOOK.uf_StartHook(TO_CHAR(SYSDATE,'YYYY-MM-DD HH24:MI:SS'),
                     i_varAPIComment,
                     l_nCount);
 o_nretcode:=SPF.PKGF_SPHOOK.uf_StopHook();
$END
null;
end;   
/

declare
l number;
begin
$IF $$ABC OR $$ABC IS NULL $THEN
L:=1;
UP_INSER(1,2);
$ELSE
RETURN;
$END
L:=2;
END;
/

--替换grouping_id
select a,b,sum(c),grouping_id(a,b) 
from t_test_grouping 
group by rollup(a,b);

--处理两个to_date函数的结果相减
select to_date('20240101123030','YYYYMMDDHH24MISS')-
to_date('20240101013030','YYYYMMDDHH24MISS') from DUAL;

select (to_date('20240101123030','YYYYMMDDHH24MISS')-
to_date('20240101013030','YYYYMMDDHH24MISS'))*24*60 col from DUAL;

--关键字作为字段名进行自定义替换
declare
x number;
begin
for rec in (select t.authid xx,max(t.authid) authid from t where t.authid=1 group by t.authid order by t.authid) loop
x:=rec.authid;
if rec.authid=1 then
null;
end if;
end loop;
end;
/

--处理数据类型映射
create or replace package pkg_test_type is
subtype x is char(10);
a constant char(11):='ccc';
a2 char(12):='yyy';
type tr is record(a char(13),b number);
type tt is table of char(14) index by pls_integer;
procedure p1(a char,b number);
function f1(a char,b number) return char;
end;
/
create or replace package body pkg_test_type is
procedure p1(a char,b number) is
d char(16);
begin
declare
e char(17);
begin
select cast (null as char(10)) into  e from dual;
end;
end;
function f1(a char,b number) return char is
begin
return null;
end;
end;
/

--处理数据字典查询
select object_name,owner,owner ow from all_objects t,dual  
where t.owner='SYS'
and lower(t.owner)=lower('SYS')
and lower(t.owner||'123')=lower('SYS')
and upper(t.owner)=upper('sys')
and t.owner like 'SYS%'
AND OBJECT_TYPE='TABLE'
and object_name='T_TABLE';
SELECT * FROM all_tab_columns WHERE OWNER='SYS' and table_name='T_TABLE';
SELECT * FROM user_types where type_name='TY_123';
SELECT * FROM user_objects where object_type='TABLE' AND OBJECT_NAME='T_TABLE';
SELECT * FROM user_tables WHERE TABLE_NAME='T_TABLE';
SELECT * FROM user_col_comments WHERE TABLE_NAME='T_TABLE';
CREATE FUNCTION F123 (A user_tables%ROWTYPE,B user_tables.TABLE_NAME%TYPE) RETURN user_tables.TABLE_NAME
IS
X user_tables%ROWTYPE;
Y user_tables.TABLE_NAME%TYPE;
BEGIN
RETURN NULL;
END;
/

--替换for update of
select t.name from t, t1 where t.id=t1.id for update of t.name nowait;
select t.name from t, t1 where t.id=t1.id for update of t1.name nowait;
select t1.name from t, t1 where t.id=t1.id for update of name nowait;
select * from t3 where t3.id=2 for update of name nowait;
select * from t3 t4 where t4.id=2 for update of name nowait;
select * from table(l_tyt),t4 ,table(l_tyt) where t4.id=2 for update of name nowait;
begin
select funcname(i1=>t2.sequenceno) bulk collect into l_tyt 
from user1.t_r_abc t1,user2.t_abc t2 
where t1.id=t2.ID
for update of t2.sequenceno NOWAIT;
end;
/
select t.name from t, t1 where t.id=t1.id for update of t.name,t.id nowait;
select t.name from t, t1 where t.id=t1.id for update of t.name,t1.name nowait;

--移除声明游标查询SQL中的into子句
declare
l number;
cursor c is
select count(1) into l from dual;
begin
null;
end;
/
DECLARE
l number;
c sys_refcursor;
BEGIN
open c for select count(1) into l from dual;
end;
/
DECLARE
l_Sql varchar2(200):='select 1 a from dual';
c sys_refcursor;
BEGIN
open c for l_Sql;
end;
/

--query_blobk替换
DECLARE
L_NCOUNT NUMBER;
 cursor l_curobject IS
  select object_type
         ,count(*) as installedcount
         ,count(decode(status,'VALID',NULL,STATUS)) AS INVALIDCOUNT
    FROM all_objects
  WHERE OWNER IN ('ABC')
  GROUP BY OBJECT_TYPE;
   cursor l_curobject2 IS
   select count(1) ct from (
  select object_type
         ,count(*) as installedcount
         ,count(decode(status,'VALID',NULL,STATUS)) AS INVALIDCOUNT
    FROM all_objects
  WHERE OWNER IN ('ABC')
  GROUP BY OBJECT_TYPE);

  cursor cur3 is 
  SELECT DISTINCT upper(t1.table_name) as table_name, T1.INDEX_NAME
   FROM USER_INDEXES T1
   WHERE T1.STATUS <> 'VALID'
   ORDER BY T1.INDEX_NAME;
BEGIN
SELECT COUNT(1)
 INTO L_NCOUNT
 FROM ALL_OBJECTS T 
 WHERE T.OWNER IN ('ABC')
 and status='INVALID';
END;
/

--对于重复的表别名弹警告
select * from tab_a t1,tab_b t1 ,tab_c t2 ;  

--处理regexp_substr第二个参数不规范的情况
select regexp_substr(rec.col,'(\${1})'),
regexp_substr(rec.col,'(\${1,2})'),
regexp_substr(rec.col,'(\${1,})'),
regexp_substr(rec.col,'(\${1*})'),
regexp_substr(rec.col,'({\${1}})') ,
regexp_substr(rec.col,'({\${1,}{1*}})'),
regexp_substr(rec.col,'({\${1,}\{1*\}})') 
from dual;


--处理固定函数表达式替换
select to_char(SYSDATE + TRIM(i_chdaternge), 'yyyymmdd') from t;

--基于hashcode的替换
select t1.* from t_hashcode t1, t_x_hashcode t1
where t1.id=t1.xid;

--替换replace函数
select replace(SRCSTR => 'a',
               OLDSUB => 'v',
               NEWSUB => 'd') from dual;

select replace(OLDSUB => 'v',
               SRCSTR => 'a',
               NEWSUB => 'd') from dual;

select replace(OLDSUB => substr('v',1,1),
               SRCSTR => replace('a','v','d'),
               NEWSUB => 'd') from dual;

select replace('a','v','d') from dual;
BEGIN
l_var:=replace(SRCSTR => 'a',OLDSUB => 'v',NEWSUB => 'd');
END;
/

--替换自定义复合类型into表达式
declare
l_ty_test ty_udt_test:=ty_udt_test();
l_ty_test2 ty_udt_test2;
BEGIN
select ty_udt_test(1,'a') into l_ty_test from dual;
select ty_udt_test(c1=>1,c2=>'a') into l_ty_test from dual;
select ty_udt_test(1, --comment
                  'a') into l_ty_test from dual;

l_ty_test2:=ty_udt_test2(1,'a');
end;
/

--处理两表left join或right join带(+)的
select * from t1 left join t2 on t1.id=t2.id(+);
select * from t1 t3 left join t2 t4 on t3.id=t4.id(+);
select * from t1 left;
select * from t1 right join t2 on t1.id(+)=t2.id;
select * from t1 t3 right join t4 on t3.id(+)=t4.id;
select * from t1 right;
select * from t1 join t2 on t1.id=t2.id(+);
select * from t1 t1 join t2 on t1.id=t2.id(+);
select * from t1 join t2 on t1.id(+)=t2.id;
select * from t1 t1 join t2 on t1.id(+)=t2.id;

select * from t1 left join t2 on t1.id(+)=t2.id;
select * from t1 right join t2 on t1.id=t2.id(+);

--处理union all里有NULL，（手动配置正则规则）
select * from (
(select NULL AS capital from dual)
union  all
select 'X' AS capital from dual);

-- 测试UDT类型在SELECT语句和非SELECT语句中的转换

-- 在SELECT语句中的UDT类型应该被转换
DECLARE
  v_result1 ty_udt_test;
  v_result2 ty_another;
BEGIN
  -- SELECT语句中的UDT类型调用应该被转换为CAST表达式
  SELECT ty_udt_test(1, 'test') INTO v_result1 FROM dual;
  SELECT ty_another(2, 'example') INTO v_result2 FROM dual;
  
  -- 子查询中的UDT类型也应该被转换
  FOR r IN (SELECT ty_udt_test(3, 'test2') AS col1 FROM dual) LOOP
    NULL;
  END LOOP;
  
  -- 非SELECT语句中的UDT类型调用不应该被转换
  v_result1 := ty_udt_test(4, 'not in select');
  v_result2 := ty_another(5, 'outside select');
  
  -- 在PL/SQL块中的赋值语句不应该被转换
  IF TRUE THEN
    v_result1 := ty_udt_test(6, 'in if block');
  END IF;
  
  -- 在函数调用参数中不应该被转换
  some_procedure(ty_udt_test(7, 'param'));

  -- 不在udtlist.properties中的UDT类型，不进行转换
  select txt_test(1,'a') into v_result1 from dual;
END;
/ 

--处理动态SQL
begin
execute immediate 'ALTER session set work_area_size_policy=AUTO';
execute immediate 'truncate table t_dynamic_sql reuse storage';
execute immediate 'select 1 from dual' into l_result;
execute immediate 'select 1 from dual where 1=$1' using 1;
end;
/

--处理q转义
select q'{select 1 from dual}' from dual;
select q'{select   '1' from dual}' from dual;
select q'{select '1  
' from dual}' from dual;
declare
l_sql varchar2(1000);
begin
l_sql:=q'{x'x}'||q'{x'x}';
l_sql:=q'[x'x]'||q'{x'x}';
l_sql:=q'{x'x}'||q'!x'x!';
end;
/

--测试给指定的包添加函数
create or replace package pkg_test_add_func IS
a number;
function x1(i_a number) return number;
end;
/
create or replace package body pkg_test_add_func IS
function x1(i_a number) return number is
begin
return i_a+1;
end;
end;
/

--正则规则，for xx in v1.first..v1.last 改 1..count
BEGIN
for xx in v1.first..v1.last loop
null;
end loop;
END;
/

--测试给指定包替换函数
create or replace package pkg_test_replace_func IS
a number;
function x1(i_a number) return number;
function x2(i_a number) return number;
procedure p1(i_a number);
end;
/
create or replace package body pkg_test_replace_func IS
function x1(i_a number) return number is
begin
return i_a+1;
end;
function x2(i_a number) return number is
begin
return i_a+2;
end;
procedure p1(i_a number) is
begin
dbms_utility.format_error_backtrace();
end;
end;
/

begin
proc('aa'||dbms_utility.format_error_backtrace());
x:=dbms_utility.format_error_backtrace();
y:=dbms_utility.get_time;
proc(1,case x when 0 then y||dbms_utility.format_call_stack else y||dbms_utility.format_error_backtrace end);
end;
/

--处理常量字符串长度不足时，自动增加长度
create package pkg_test_cons is
c_ok constant varchar2(4) := '通过';
c_not_ok constant varchar2(9) := '不通过';
end;
/ 

--处理update set return 改成returning
update t set name='a' where id=1 return name into l_name;
BEGIN
update t set name='a' where id=1 return name into l_name;
insert into t(id,name) values(1,'a') return id into l_id;
delete from t where id=1 return id into l_id;
END;
/

