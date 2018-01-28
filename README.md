# Description
This is an kotlin app that compiles and runs the program that is written in S language.
S language is a simple programming language initroduced in Martin Davis's computability book.
So by definition in that book this app is a universal program that can runs every programs in S language.

# Some intresting features

-This app can detect loop in the code and breaking the execution.

-Can show the syntax and lexical errors index.
	
-Can stop after exceeding the limit of user specified max epochs argument
	
-(future) Do macro expansion (use previously macros written in S language)


# Usage
in the root folder of project simply run "java -jar universal.jar [args]"
	
where args is:

	-p [file name of program written in S to compile and run (placed in the root folder)]
			
	`[x1 (an integer which means first argument of program to run )]...[xn]`
		
by running the "java -jar universal.jar -p sample_input.slang 1 3" u can watch the demo that runs the program of sample_input.slang file with args x1=1 and x2=3

@by Mohsen Rezaei as a project for Theory of computer science lecture (2018-01-18)
