DECLARE
  v_count NUMBER;
  v_tyt_a typ_a :=typ_a();
BEGIN
  SELECT COUNT(*) INTO v_count FROM dual;
  OPEN cur_test FOR
    SELECT * FROM table1;
  FETCH cur_test INTO v_count;
END; 
