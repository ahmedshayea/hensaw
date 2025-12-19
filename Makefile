.PHONY: dev build down proto clean test

dev:
	docker-compose up

build:
	docker-compose build

down:
	docker-compose down

proto:
	# Placeholder for proto generation command
	# You will need to install protoc and plugins or use a dockerized generator
	@echo "Proto generation not yet implemented. Please define generation logic."

clean:
	docker-compose down -v
	rm -rf data/

test:
	@echo "Run tests in each service directory"
