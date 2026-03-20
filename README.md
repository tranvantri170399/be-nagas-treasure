# BE Game nags

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd be-game-nags
```

### 2. Configure Environment Variables

The application uses environment variables for configuration. Create a `.env` file in the root of the project by copying the example file:

```bash
cp .env.example .env
```

Now, open the `.env` file and customize the values for your environment. At a minimum, you will likely need to configure the following:

*   `MONGODB_URI`: The connection string for your MongoDB instance.
*   `REDIS_HOST`: The hostname of your Redis server.
*   `BALANCE_SERVICE_BASE_URL`: The base URL for the external balance service.

### 3. Build the Application

The project is built using the Gradle wrapper. This command will also compile your Protocol Buffer files into Java code.

```bash
./gradlew build
```

### 4. Run with Docker Compose

The easiest way to run the application and its dependencies is with Docker Compose.

```bash
docker-compose up -d
```

The service will be available at `http://localhost:3000`.

## gRPC & Protocol Buffers

This project uses gRPC for efficient, strongly-typed API communication.

*   The API contracts are defined in `.proto` files located under `app/src/main/proto/`.
*   The Gradle build process is configured to automatically generate the necessary Java gRPC service stubs and message classes from these `.proto` files.
*   If you make any changes to a `.proto` file, simply rebuild the project (`./gradlew build`) to have the corresponding Java code regenerated.

## Code Quality

This project uses several tools to maintain code quality:

*   **Checkstyle**: Enforces a consistent coding style.
*   **PMD**: Scans for common programming flaws.
*   **SpotBugs**: Analyzes code for potential bugs.

To run all quality checks:

```bash
./gradlew check
```

Reports for each tool are generated in the `app/build/reports` directory.

## Contributing

Contributions are welcome! If you'd like to contribute, please follow these steps:

1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Make your changes and ensure all tests and quality checks pass.
4.  Submit a pull request with a clear description of your changes.
