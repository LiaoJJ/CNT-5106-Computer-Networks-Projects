command 

javac *.java

java fileServer 5000 test.pdf

java peer 5000 5001 5002
java peer 5000 5002 5003
java peer 5000 5003 5004
java peer 5000 5004 5005
java peer 5000 5005 5001


history:
2.1 now it will create a SummaryFile.txt each time it receive  a file chunk.