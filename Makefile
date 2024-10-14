# Makefile for COMP 412 Lab 1


clean:
	rm -rf *.class

# Compile Java code into .class files
build: 
	javac -d . Main.java 
#chmod +x 412alloc

# Create a TAR file
#tar: build
#tar cvf eam20.tar .
#mv eam20.tar ../TarFileGoesHere





	
