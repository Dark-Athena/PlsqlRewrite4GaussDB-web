--����new������
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
--���� table()
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
--����ע��
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

--���� �������� $IF
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

--����ȫ�Ƕ��ź�ȫ�ǿո�
select 1��2,3 from dual;
select func(a��b) from dual;
select func(i=>a��o=>b,io=>c)from dual;
select 1  ��from dual;
select����1����from����dual����;����
select 1��2,3 from dual��dual,dual where a in (1��2,3) group by 1��2,3 order by 1��2,3;
insert into tab(c1��c2,c3) values (1��2,3);

--����ȫ������
select * from t where ��1=2 and 2=2�� and ��1=2 and 2=2��;

--����ؼ�����Ϊ�ֶα���
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

-- �ֶα�����˫���Ų��ܱ���
select 1 "a" , 2 "��" from dual;

--�����ظ������ı���
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

--����lag��lead
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

--����nocopy
create procedure t(a in out  nocopy number,
b in number) is
begin
null;
end;
/
select 1 as nocopy from dual;

--����unique
select unique(a) from t;

--����pls_integer
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

--����not member of
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

--����having��group byǰ��
select count(1) from t having count(1)>1 group by id; 
select id,name,count(1) from t having count(1)>1 group by id,name; 
select id,name,count(1) from t where 1=1 having count(1)>sum(1) and count(1)<avg(1) group by id,name; 
select id,name,count(123) from t where 1=1 group by id,name having count(123)>sum(123) and count(123)<=avg(123); 
select count(1) from t having count(1)>1;
select count(1) from t group by a;
select count(1) from t 
    group by id
   having count(1)>1; 


--���� xmlagg(xmlparse(content {expression} wellformed)).getclobval() ,�Ƴ�getclobval��wellformed
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

--����result_cache relies_on
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

--����deterministic
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
--�������ƴ��ν���ע��
select func(a=> /*b*/ 1) from dual;
select func(a=>/*b*/ 1) from dual;
select func(a=>--TEST
 1) from dual;

--����=>- ��=>+
select func(a=>-1) from dual;
select func(a=>+1) from dual;

--�����쳣����ӳ�䣨���������ļ�excption_mapping.properties��
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

--����dml���end case
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

--����raise_application_error
begin
raise_application_error(-20001,'a');
raise_application_error(-20001,'a'||'b');
raise_application_error(-20001,'a'||'b'||func('a','b'));
end;
/

--����SYS_CONTEXT
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

--����(tablename)
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

--����using table()
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

--����߼���
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
dbms_obfuscation_toolkit.md5(input_string=>x,checksum_string=>z); --��֧�֣��޿�ӳ�亯��
n:=dbms_obfuscation_toolkit.md5(input=>y);
x:=rawtohex(dbms_obfuscation_toolkit.md5(input=>y));
select dbms_obfuscation_toolkit.md5(input=>y) into n from dual;
z:=dbms_obfuscation_toolkit.md5(input_string=>x); --��֧�֣��޿�ӳ�亯��
end;
/

--���������滻
declare
v1 ty_test;
v2 ty_test2:= ty_test2();
v3 tyt_test:= tyt_test();
x text;
BEGIN
v1:=ty_test2(2,'b') ;
v3(1):=v1;
select ty_test2(1,'a') into v2 from dual ;
--v1.up_copytype(v2); --ע�Ͳ�Ҫ�滻
v1.up_copytype(v2);--v1.up_copytype(v2);
v3(2).up_copytype(v3(1));
x:=v1.uf_tostring();
x:='a' || v1.uf_tostring();
--x:=v1.uf_tostring(9); --û������÷�
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

--�滻grouping_id
select a,b,sum(c),grouping_id(a,b) 
from t_test_grouping 
group by rollup(a,b);

--��������to_date�����Ľ�����
select to_date('20240101123030','YYYYMMDDHH24MISS')-
to_date('20240101013030','YYYYMMDDHH24MISS') from DUAL;

select (to_date('20240101123030','YYYYMMDDHH24MISS')-
to_date('20240101013030','YYYYMMDDHH24MISS'))*24*60 col from DUAL;

--�ؼ�����Ϊ�ֶ��������Զ����滻
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

--������������ӳ��
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

--���������ֵ��ѯ
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

--�滻for update of
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

--�Ƴ������α��ѯSQL�е�into�Ӿ�
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

--query_blobk�滻
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

--�����ظ��ı����������
select * from tab_a t1,tab_b t1 ,tab_c t2 ;  

--����regexp_substr�ڶ����������淶�����
select regexp_substr(rec.col,'(\${1})'),
regexp_substr(rec.col,'(\${1,2})'),
regexp_substr(rec.col,'(\${1,})'),
regexp_substr(rec.col,'(\${1*})'),
regexp_substr(rec.col,'({\${1}})') ,
regexp_substr(rec.col,'({\${1,}{1*}})'),
regexp_substr(rec.col,'({\${1,}\{1*\}})') 
from dual;


--����̶��������ʽ�滻
select to_char(SYSDATE + TRIM(i_chdaternge), 'yyyymmdd') from t;

--����hashcode���滻
select t1.* from t_hashcode t1, t_x_hashcode t1
where t1.id=t1.xid;

--�滻replace����
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

--�滻�Զ��帴������into���ʽ
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

--��������left join��right join��(+)��
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

--����union all����NULL�����ֶ������������
select * from (
(select NULL AS capital from dual)
union  all
select 'X' AS capital from dual);

-- ����UDT������SELECT���ͷ�SELECT����е�ת��

-- ��SELECT����е�UDT����Ӧ�ñ�ת��
DECLARE
  v_result1 ty_udt_test;
  v_result2 ty_another;
BEGIN
  -- SELECT����е�UDT���͵���Ӧ�ñ�ת��ΪCAST���ʽ
  SELECT ty_udt_test(1, 'test') INTO v_result1 FROM dual;
  SELECT ty_another(2, 'example') INTO v_result2 FROM dual;
  
  -- �Ӳ�ѯ�е�UDT����ҲӦ�ñ�ת��
  FOR r IN (SELECT ty_udt_test(3, 'test2') AS col1 FROM dual) LOOP
    NULL;
  END LOOP;
  
  -- ��SELECT����е�UDT���͵��ò�Ӧ�ñ�ת��
  v_result1 := ty_udt_test(4, 'not in select');
  v_result2 := ty_another(5, 'outside select');
  
  -- ��PL/SQL���еĸ�ֵ��䲻Ӧ�ñ�ת��
  IF TRUE THEN
    v_result1 := ty_udt_test(6, 'in if block');
  END IF;
  
  -- �ں������ò����в�Ӧ�ñ�ת��
  some_procedure(ty_udt_test(7, 'param'));

  -- ����udtlist.properties�е�UDT���ͣ�������ת��
  select txt_test(1,'a') into v_result1 from dual;
END;
/ 

--����̬SQL
begin
execute immediate 'ALTER session set work_area_size_policy=AUTO';
execute immediate 'truncate table t_dynamic_sql reuse storage';
execute immediate 'select 1 from dual' into l_result;
execute immediate 'select 1 from dual where 1=$1' using 1;
end;
/

--����qת��
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

--���Ը�ָ���İ���Ӻ���
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

--�������for xx in v1.first..v1.last �� 1..count
BEGIN
for xx in v1.first..v1.last loop
null;
end loop;
END;
/

--���Ը�ָ�����滻����
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

--�������ַ������Ȳ���ʱ���Զ����ӳ���
create package pkg_test_cons is
c_ok constant varchar2(4) := 'ͨ��';
c_not_ok constant varchar2(9) := '��ͨ��';
end;
/ 

--����update set return �ĳ�returning
update t set name='a' where id=1 return name into l_name;
BEGIN
update t set name='a' where id=1 return name into l_name;
insert into t(id,name) values(1,'a') return id into l_id;
delete from t where id=1 return id into l_id;
END;
/

