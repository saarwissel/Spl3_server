all: clean bin_dir compile link run

bin_dir:
	mkdir -p bin

link: compile
	@echo "Linking binary..."
	g++ -std=c++11 -o bin/StompClient bin/StompClient.o bin/event.o bin/ConnectionHandler.o bin/StompMessageBuilder.o bin/StompProtocol.o -lpthread
 


compile: src/StompClient.cpp src/event.cpp src/ConnectionHandler.cpp src/StompMessageBuilder.cpp src/StompProtocol.cpp
	g++ -g -c -Wall -Weffc++ -std=c++11 -Iinclude -o bin/StompClient.o src/StompClient.cpp
	g++ -g -c -Wall -Weffc++ -std=c++11 -Iinclude -o bin/event.o src/event.cpp
	g++ -g -c -Wall -Weffc++ -std=c++11 -Iinclude -o bin/ConnectionHandler.o src/ConnectionHandler.cpp
	g++ -g -c -Wall -Weffc++ -std=c++11 -Iinclude -o bin/StompMessageBuilder.o src/StompMessageBuilder.cpp
	g++ -g -c -Wall -Weffc++ -std=c++11 -Iinclude -o bin/StompProtocol.o src/StompProtocol.cpp

run:
	./bin/StompClient

clean:
	@echo "Cleaning bin directory"
	rm -rf bin/*
