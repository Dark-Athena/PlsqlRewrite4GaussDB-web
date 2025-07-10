--处理new构造器
declare
l_ty_a ty_a:= ty_a(null,null);
l_tyt_a tyt_a:= tyt_a();
l_tyt_varchar tyt_varchar:= tyt_varchar();
l_tyt_varchar2 tyt_varchar:=
                                 tyt_varchar();
l_tyt_b tyt_a:= tyt_a();
l_tyt_C tyt_a := tyt_a();
l_tyt_Cd tyt_a := tyt_a();

x int;
begin
l_tyt_varchar := tyt_varchar();
l_tyt_varchar := tyt_varchar();
l_tyt_varchar := tyt_varchar();
--处理 table()
select count(1) into x from (select * from unnest_table(l_tyt_varchar) column_value) t where t.column_value ='a';

select count(1) into x from (select * from unnest_table(l_tyt_a) column_value) t where t.a ='1';
select count(1) into x from (select * from unnest_table(l_tyt_varchar) column_value) t where t.column_value ='a';

select count(1) into x from (select * from unnest_table(l_tyt_a) column_value) t where t.a ='1';

select count(1) into x from t123 t,(select * from unnest_table(l_tyt_varchar) column_value) p where t.c1=p.column_value; 
select count(1) into x from t123 t where exists (select 1 from (select * from unnest_table(l_tyt_varchar) column_value) p where t.c1=p.column_value); 

select count(1) into x from t123 t where exists (select 1 from (select * from unnest_table(l_tyt_varchar) column_value) where t.c1=column_value); 

select count(1) into x from t123 t where exists (select 1 from (select * from unnest_table(l_tyt_a) column_value) p where t.c1=p.a); 

select count(1) into x from table123; 
end;
/


DECLARE
--l_ty_test ty_test:= ty_test();
l_ty_test2 ty_test2:= ty_test2();
l_ty_r_test ty_r_test:= ty_r_test();
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
l_ty_r_test ty_r_test:= ty_r_test();
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
select * /**/ from dual;
select * /*/*/ from dual;
select * /***/ from dual;
select * /**/ from dual;
select * /*111*/ from dual;
select * /*22*2*/ from dual;
select * /*33
*4*/ from dual;
select * --/*aaa*/
 from dual;
select * --/*a
/*aa*/ from dual;
select /* 1/2 */ 1 from dual;

--处理 条件编译 $IF
declare
l number;
begin
--$IF $$ABC OR $$ABC IS NULL --$THEN
L:=1;
UP_INSER(1,2);
--$ELSE
RETURN;
--$END
L:=2;
END;
/

--处理全角逗号和全角空格
select 1,2,3 from dual;
select func(a,b) from dual;
select func(i=>a,o=>b,io=>c)from dual;
select 1   from dual;
select  1  from  dual  ;  
select 1,2,3 from dual,dual,dual where a in (1,2,3) group by 1,2,3 order by 1,2,3;
insert into tab(c1,c2,c3) values (1,2,3);

--处理全角括号
select * from t where (1=2 and 2=2) and (1=2 and 2=2);

--处理关键字作为字段别名
select 1 as language from dual;
select 1  AS language from dual;
select 1 AS SPECIFICATION from dual;
select 1 AS OPERATOR from dual;
select 1 AS POSITION from dual;
select sum(1)  AS POSITION from dual;
select trim('aa')||trim('bb') AS position from dual;

SELECT 1 FROM (
SELECT DENSE_RANK() OVER(ORDER BY 1) AS DENSE_RANK FROM DUAL)
GROUP BY DENSE_RANK;
select case when 1=1 then 1 end AS DENSE_RANK from dual;

-- 字段别名带双引号不能报错
select 1 "a" , 2 "啊" from dual;

--处理重复声明的变量
declare

y varchar2(10);
x number;      
begin
null;
declare

y varchar2(10);
x number;      
begin
null;
end; 
end; 
/
declare

y varchar2(10);
x number;      
begin
null;
end; 
/
declare

y varchar2(10);
x int;      
begin
null;
end; 
/

create package body test_pkg is 
procedure a is

y varchar2(10);
x number;      
begin
 null;
 declare
  
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
select lag(a,1,'1') over(order by c) from t;  
select lag(a,1,1.1) over(order by c) from t;  
select lag(a,1,'1') over(order by c) from t;  
select lag(a,1) over(order by c) from t;  
select lag(a/b,1,'1') over(order by c) from t;  
select lag(ceil(a/b),1,'1') over(order by c) from t;  

select lead(a,1,'1') over(order by c) from t;  
select lead(a,1,'1') over(order by c) from t;  
select lead(a,1) over(order by c) from t;  
select lead(a/b,1,'1') over(order by c) from t;  
select lead(ceil(a/b),1,'1') over(order by c) from t; 

--处理nocopy
create procedure t(a in out   number,
b in number) is
begin
null;
end;
/
select 1 as nocopy from dual;

--处理unique
select distinct(a) from t;

--处理pls_integer
declare
type x is table of int index by binary_integer;
begin
  null;
  end;
/
select 1 as pls_integer from dual;
create function b(a binary_integer,a2 number) return binary_integer is
type x is table of int index by binary_integer;
c binary_integer;
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
if not (L_TYT_A(1).B = any(L_TYT_VARCHAR2)) then 
  dbe_output.put_line('Y');
  else 
  dbe_output.put_line('N');
end if;
if  (L_TYT_A(1).B = any(L_TYT_VARCHAR2)) then 
  dbe_output.put_line('Y');
  else 
  dbe_output.put_line('N');
end if;

L:=CASE WHEN not (L_TYT_A(1).B = any(L_TYT_VARCHAR2)) THEN 1 END;
L:=CASE WHEN  (L_TYT_A(1).B = any(L_TYT_VARCHAR2)) THEN 1 END;
end;
/

--处理having在group by前面
select count(1) from t  group by id having count(1)>1; 
select id,name,count(1) from t  group by id,name having count(1)>1; 
select id,name,count(1) from t where 1=1  group by id,name having count(1)>sum(1) and count(1)<avg(1); 
select id,name,count(123) from t where 1=1 group by id,name having count(123)>sum(123) and count(123)<=avg(123); 
select count(1) from t having count(1)>1;
select count(1) from t group by a;
select count(1) from t 
    group by id
   having count(1)>1; 


--处理 xmlagg(xmlparse(content {expression} wellformed)).getclobval() ,移除getclobval和wellformed
select a,rtrim(xmlagg(xmlparse(content trim(b)||',' ) order by b)::clob,',') from t;
select a,rtrim(xmlagg(xmlparse(content trim(b)||',' ) order by b)::clob,',') from t;
select a::clob from dual;
DECLARE
x varchar2(10);
y xml;
BEGIN
x:=y::clob;
end;
/

--处理result_cache relies_on
create function uf return number 
IMMUTABLE IS
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
create package pkgs_test_result IS
function uf return number immutable ;
end;
/
CREATE PACKAGE BODY PKGS_TEST_RESULT IS
function uf return number IMMUTABLE IS
BEGIN
return 1;
end;
END;
/

--处理deterministic
create function uf return number 
immutable IS
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
function uf return number immutable;
end;
/
CREATE PACKAGE BODY P IS 
function uf return number 
immutable IS
BEGIN
return 1;
end;
END;
/ 
--处理按名称传参紧接注释
select func(a=> /*b*/ 1) from dual;
select func(a=> /*b*/ 1) from dual;
select func(a=> --TEST
 1) from dual;

--处理=>- 和=>+
select func(a=> -1) from dual;
select func(a=> +1) from dual;

--处理异常名称映射（基于配置文件excption_mapping.properties）
begin
  null;
  exception when pkgs_e.lock_error then
    null;
end;
/
begin
  null;
  exception when data_exception then
    null;
end;
/

--处理dml里的end case
select 1 AS case from dual;
select case when 1=1 then 1 end AS case from dual;
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
report_application_error('a',-20001);
report_application_error('a'||'b',-20001);
report_application_error('a'||'b'||func('a','b'),-20001);
end;
/

--处理SYS_CONTEXT
select pg_current_sessid() col1,
pg_current_sessid() col2,
inet_out(inet_client_addr()) col3,
current_setting('pgxc_node_name') col4
 from dual;

declare
a varchar2(200);
begin
a:=pg_current_sessid();
a:=inet_out(inet_client_addr());
a:=current_setting('pgxc_node_name');
end;
/

--处理(tablename)
select 1 from t1 join  t2  on t1.id=t2.id;
select 1 from t1 join t2  on t1.id=t2.id;
select 1 from t1 join t2 on t1.id=t2.id;
select 1 from t1 join (select id from t2) t3 on t1.id=t3.id; 
select 1 from t1 join (t2 join t4 on t2.id=t4.id) t3 on t1.id=t3.id; 
DECLARE
x int;
begin
select 1 
into x 
from (select * from unnest_table(v) column_value) t1 
LEFT JOIN  t_xxx  t2
on t1.id=t2.id;
end;
/
select 1 from (select * from unnest_table(v) column_value) t1 LEFT JOIN  t_xxx  t2 on t1.id=t2.id;
select 1 from (select * from unnest_table(v) column_value) t1 JOIN  t_xxx  t2 on t1.id=t2.id;

--处理using table()
merge into t1
using (select * from unnest_table(i_tyt) column_value) t2
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
dbe_output.put_line('a');
x:=dbe_utility.format_error_backtrace();
dbe_application_info.set_moudle('xxx','yyy');
n := hextoraw(md5(dbe_raw.cast_to_varchar2(y)));
dbms_obfuscation_toolkit.md5(input_string=>x,checksum_string=>z); --不支持，无可映射函数
n:=hextoraw(md5(dbe_raw.cast_to_varchar2(y)));
x:=rawtohex(hextoraw(md5(dbe_raw.cast_to_varchar2(y))));
select hextoraw(md5(dbe_raw.cast_to_varchar2(y))) into n from dual;
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
select cast((1,'a') as ty_test2) into v2 from dual ;
--v1.up_copytype(v2); --注释不要替换
pkgs_typecopy.up_copytype(v2,v1);--v1.up_copytype(v2);
pkgs_typecopy.up_copytype(v3(1),v3(2));
x:=pkgs_ty2string.uf_ty2string(v1);
x:='a' || pkgs_ty2string.uf_ty2string(v1);
--x:=v1.uf_tostring(9); --没有这个用法
v3.extend;
v3(2):=v2;

for i in 1..v3.count loop
x:=pkgs_ty2string.uf_ty2string(v3(i));
X:=X||'ABC'||pkgs_ty2string.uf_ty2string(v3(i));
pkgs_logmanager.up_Debug(l_var, 'i_ty:'||pkgs_ty2string.uf_ty2string(l_ty));
pkgs_logmanager.up_Debug(l_var,pkgs_ty2string.uf_ty2string(l_ty));

x:=substrb('xxxx'||
 pkgs_ty2string.uf_ty2string(i_tysystemenv)
 ||',' ,1,4000);

 x:=substrb('xxxx'
 ||pkgs_ty2string.uf_ty2string(i_tysystemenv)
 ||',' ,1,4000);

  x:=substrb('xxxx'
 ||pkgs_ty2string.uf_ty2string(i_tysystemenv) ||','
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
pkgs_typecopy.up_copytype(v2,v1);
pkgs_typecopy.up_copytype(v3,v1);
pkgs_typecopy.up_copytype(v3(1),v3(2));
pkgs_typecopy.up_copytype(v3(5),v3(2));
pkgs_typecopy.up_copytype(v4,v3(2));
pkgs_typecopy.up_copytype(v2341(iii),v3(2));
x:=pkgs_ty2string.uf_ty2string(v1);
for i in 1..v3.count loop
x:=pkgs_ty2string.uf_ty2string(v3(i));
X:=X||'ABC'||pkgs_ty2string.uf_ty2string(v3(i));
end loop;
end;
/

declare
x number;
BEGIN
--$IF $$ABC OR $$ABC IS NULL --$THEN
 O_NRETCODE := null/*SPF.PKGF_SPHOOK.uf_StartHook(TO_CHAR(SYSDATE,'YYYY-MM-DD HH24:MI:SS'),i_varAPIComment,l_nCount)*/;
 o_nretcode:=null/*SPF.PKGF_SPHOOK.uf_StopHook()*/;
--$END
null;
end;   
/

declare
l number;
begin
--$IF $$ABC OR $$ABC IS NULL --$THEN
L:=1;
UP_INSER(1,2);
--$ELSE
RETURN;
--$END
L:=2;
END;
/

--替换grouping_id
select a,b,sum(c),grouping(a,b) 
from t_test_grouping 
group by rollup(a,b);

--处理两个to_date函数的结果相减
select intervaltonum(to_date('20240101123030','YYYYMMDDHH24MISS')-
to_date('20240101013030','YYYYMMDDHH24MISS')) from DUAL;

select (intervaltonum(to_date('20240101123030','YYYYMMDDHH24MISS')-
to_date('20240101013030','YYYYMMDDHH24MISS')))*24*60 col from DUAL;

--关键字作为字段名进行自定义替换
declare
x number;
begin
for rec in (select t.auth_id xx,max(t.auth_id) AS auth_id from t where t.auth_id=1 group by t.auth_id order by t.auth_id) loop
x:=rec.auth_id;
if rec.auth_id=1 then
null;
end if;
end loop;
end;
/

--处理数据类型映射
create or replace package pkg_test_type is
subtype x is varchar2(10);
a constant varchar2(11):='ccc';
a2 varchar2(12):='yyy';
type tr is record(a varchar2(13),b number);
type tt is table of varchar2(14) index by binary_integer;
procedure p1(a varchar2,b number);
function f1(a varchar2,b number) return varchar2;
end;
/
create or replace package body pkg_test_type is
procedure p1(a varchar2,b number) is
d varchar2(16);
begin
declare
e varchar2(17);
begin
select cast (null as varchar2(10)) into  e from dual;
end;
end;
function f1(a varchar2,b number) return varchar2 is
begin
return null;
end;
end;
/

--处理数据字典查询
select upper(object_name) as object_name,upper(schema) as owner,upper(schema) ow from db_objects t,dual  
where upper(t.schema)='SYS'
and lower(t.schema)=lower('SYS')
and lower(t.schema||'123')=lower('SYS')
and upper(t.schema)=upper('sys')
and upper(t.schema) like 'SYS%'
AND upper(object_type)='TABLE'
and upper(object_name)='T_TABLE';
SELECT * FROM db_tab_columns WHERE upper(schema)='SYS' and upper(table_name)='T_TABLE';
SELECT * FROM my_types where upper(type_name)='TY_123';
SELECT * FROM my_objects where upper(object_type)='TABLE' AND upper(object_name)='T_TABLE';
SELECT * FROM my_tables WHERE upper(table_name)='T_TABLE';
SELECT * FROM my_col_comments WHERE upper(table_name)='T_TABLE';
CREATE FUNCTION F123 (A my_tables%ROWTYPE,B my_tables.TABLE_NAME%TYPE) RETURN my_tables.TABLE_NAME
IS
X my_tables%ROWTYPE;
Y my_tables.TABLE_NAME%TYPE;
BEGIN
RETURN NULL;
END;
/

--替换for update of
select t.name from t, t1 where t.id=t1.id for update of t nowait;
select t.name from t, t1 where t.id=t1.id for update of t1 nowait;
select t1.name from t, t1 where t.id=t1.id for update of t1 nowait;
select * from t3 where t3.id=2 for update of t3 nowait;
select * from t3 t4 where t4.id=2 for update of t4 nowait;
select * from (select * from unnest_table(l_tyt) column_value),t4 ,(select * from unnest_table(l_tyt) column_value) where t4.id=2 for update of t4 nowait;
begin
select funcname(i1=>t2.sequenceno) bulk collect into l_tyt 
from user1.t_r_abc t1,user2.t_abc t2 
where t1.id=t2.ID
for update of t2 NOWAIT;
end;
/
select t.name from t, t1 where t.id=t1.id for update of t,t nowait;
select t.name from t, t1 where t.id=t1.id for update of t,t1 nowait;

--移除声明游标查询SQL中的into子句
declare
l number;
cursor c is
select count(1)  from dual;
begin
null;
end;
/
DECLARE
l number;
c sys_refcursor;
BEGIN
open c for select count(1)  from dual;
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
         ,count(decode(status,'valid',NULL,STATUS)) AS INVALIDCOUNT
    FROM db_objects
  WHERE OWNER IN ('ABC')
  GROUP BY OBJECT_TYPE;
   cursor l_curobject2 IS
   select count(1) ct from (
  select object_type
         ,count(*) as installedcount
         ,count(decode(status,'valid',NULL,STATUS)) AS INVALIDCOUNT
    FROM db_objects
  WHERE OWNER IN ('ABC')
  GROUP BY OBJECT_TYPE);

  cursor cur3 is 
  SELECT DISTINCT upper(t1.table_name) as table_name, T1.INDEX_NAME
   FROM MY_INDEXES t1
   WHERE T1.STATUS <> 'valid'
   ORDER BY T1.INDEX_NAME;
BEGIN
SELECT COUNT(1)
 INTO L_NCOUNT
 FROM DB_OBJECTS T
 WHERE T.OWNER IN ('ABC')
 and status='invalid';
END;
/

--对于重复的表别名弹警告
select * from tab_a t1,tab_b t1 ,tab_c t2 ;  

--处理regexp_substr第二个参数不规范的情况
select regexp_substr(rec.col,'(\${1})'),
regexp_substr(rec.col,'(\${1,2})'),
regexp_substr(rec.col,'(\${1,})'),
regexp_substr(rec.col,'(\$\{1*\})'),
regexp_substr(rec.col,'(\{\${1}\})') ,
regexp_substr(rec.col,'(\{\${1,}\{1*\}\})'),
regexp_substr(rec.col,'(\{\${1,}\{1*\}\})') 
from dual;


--处理固定函数表达式替换
select to_char(SYSDATE + to_number(TRIM(i_chdaternge)), 'yyyymmdd') from t;

--基于hashcode的替换
select t1.* from t_hashcode t1, t_x_hashcode t22  --中文注释
where t1.id=t22.xid;

--替换replace函数
select replace('a', 'v', 'd') from dual;

select replace('a', 'v', 'd') from dual;

select replace(replace('a','v','d'), substr('v',1,1), 'd') from dual;

select replace('a','v','d') from dual;
BEGIN
l_var:=replace('a', 'v', 'd');
END;
/

--替换自定义复合类型into表达式
declare
l_ty_test ty_udt_test:=ty_udt_test();
l_ty_test2 ty_udt_test2;
BEGIN
select cast((1,'a') as ty_udt_test) into l_ty_test from dual;
select ty_udt_test(c1=>1,c2=>'a') into l_ty_test from dual;
select cast((1, --comment
                  'a') as ty_udt_test) into l_ty_test from dual;

l_ty_test2:=ty_udt_test2(1,'a');
end;
/

--处理两表left join或right join带(+)的
select * from t1 left join t2 on t1.id=t2.id;
select * from t1 t3 left join t2 t4 on t3.id=t4.id;
select * from t1 left;
select * from t1 right join t2 on t1.id=t2.id;
select * from t1 t3 right join t4 on t3.id=t4.id;
select * from t1 right;
select * from t1 join t2 on t1.id=t2.id(+);
select * from t1 t1 join t2 on t1.id=t2.id(+);
select * from t1 join t2 on t1.id(+)=t2.id;
select * from t1 t1 join t2 on t1.id(+)=t2.id;

select * from t1 left join t2 on t1.id(+)=t2.id;
select * from t1 right join t2 on t1.id=t2.id(+);

--处理union all里有NULL，（手动配置正则规则）
select * from (
(select cast(NULL as number) AS capital from dual)
union  all
select 'X' AS capital from dual);

-- 测试UDT类型在SELECT语句和非SELECT语句中的转换

-- 在SELECT语句中的UDT类型应该被转换
DECLARE
  v_result1 ty_udt_test;
  v_result2 ty_another;
BEGIN
  -- SELECT语句中的UDT类型调用应该被转换为CAST表达式
  SELECT cast((1, 'test') as ty_udt_test) INTO v_result1 FROM dual;
  SELECT cast((2, 'example') as ty_another) INTO v_result2 FROM dual;
  
  -- 子查询中的UDT类型也应该被转换
  FOR r IN (SELECT cast((3, 'test2') as ty_udt_test) AS col1 FROM dual) LOOP
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
/* Commented out execute_immediate due to matching remove rule: (?i)alter\s+session\s+set
execute immediate 'ALTER session set work_area_size_policy=AUTO'; */
execute immediate 'truncate table t_dynamic_sql';
execute immediate 'select 1 from dual' into l_result;
execute immediate 'select 1 from dual where 1=$1' using 1;
end;
/

--处理q转义
select 'select 1 from dual' from dual;
select 'select   ''1'' from dual' from dual;
select 'select ''1  
'' from dual' from dual;
declare
l_sql varchar2(1000);
begin
l_sql:='x''x'||'x''x';
l_sql:='x''x'||'x''x';
l_sql:='x''x'||'x''x';
end;
/

--测试给指定的包添加函数
create or replace package pkg_test_add_func IS
a number;
function x1(i_a number) return number;
/*aaa*/
function add_func(a number);
end;
/
create or replace package body pkg_test_add_func IS
function x1(i_a number) return number is
begin
return i_a+1;
end;
/*aaa*/
function add_func(a number) is 
begin
return a;
end;
end;
/

--正则规则，for xx in v1.first..v1.last 改 1..count
BEGIN
for xx in 1..v1.count loop
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
return i_a+1+1;--test replace
end;
procedure p1(i_a number) is
begin
null;--test replace
end;
end;
/

begin
proc('aa'||dbe_utility.format_error_backtrace());
x:=dbe_utility.format_error_backtrace();
y:=dbe_utility.get_time();
proc(1,case x when 0 then y||dbe_utility.format_call_stack() else y||dbe_utility.format_error_backtrace() end);
end;
/

--处理常量字符串长度不足时，自动增加长度
create package pkg_test_cons is
c_ok constant varchar2(6) := '通过';
c_not_ok constant varchar2(9) := '不通过';
end;
/ 

--处理update set return 改成returning
update t set name='a' where id=1 RETURNING name into l_name;
BEGIN
update t set name='a' where id=1 RETURNING name into l_name;
insert into t(id,name) values(1,'a') RETURNING id into l_id;
delete from t where id=1 RETURNING id into l_id;
END;
/

