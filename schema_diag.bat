cd /d D:\AndroidStudio\Project_Pandora

echo ====== [A] history of main schema file ======
git --no-pager log --oneline --all -- pandora-backend/src/main/resources/db/h2_schema.sql

echo ====== [B] which commit added the two ALTER lines in main schema ======
git --no-pager log --oneline --all -S "actor_id" -- pandora-backend/src/main/resources/db/h2_schema.sql
git --no-pager log --oneline --all -S "created_by" -- pandora-backend/src/main/resources/db/h2_schema.sql

echo ====== [C] which schema file does the test harness load ======
findstr /s /n /i "h2_schema" pandora-backend\src\test\resources\*.* pandora-backend\src\test\java\*.java
type pandora-backend\src\test\resources\application*.yml 2>nul

echo ====== [D] is test_package tracked by git ======
git --no-pager status --short test_package
git --no-pager log --oneline --all -- test_package

echo ====== [E] how the app loads schema at runtime ======
findstr /n /i "schema sql.init jdbc" pandora-backend\src\main\resources\application.yml

echo ====== done ======
