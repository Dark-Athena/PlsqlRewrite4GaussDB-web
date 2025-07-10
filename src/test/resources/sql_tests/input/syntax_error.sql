
-- 这是一个包含语法错误的SQL文件
CREATE OR REPLACE PROCEDURE test_proc
AS
BEGIN
  -- 错误1：WHERE子句后面没有条件
  SELECT * FROM missing_table WHERE;
  
  IF x > 10 THEN
    DBMS_OUTPUT.PUT_LINE('x is greater than 10');
  END; -- 错误2：缺少END IF
  
  -- 错误3：错误的语法结构
  SELECT FROM dual;
END;
/
