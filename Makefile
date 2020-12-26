scalafmtCheck:
	sbt scalafmtCheckAll

doit:
	sbt test

clean:
	git clean -fd

jni:
# Generate headers with "sbt javah"
# Create makefile with "sbt nativeInit"
# Compile shared object file with "sbt nativeCompile"
	sbt "project native; javah; nativeInit cmake chisel-uvm; nativeCompile"
