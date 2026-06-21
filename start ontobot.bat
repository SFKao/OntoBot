for /f "tokens=1,* delims==" %%a in (.env) do (
    set %%a=%%b
)

C:\Java\jdk-21.0.1\bin\java.exe -jar target\ontobot-1.0-SNAPSHOT.jar