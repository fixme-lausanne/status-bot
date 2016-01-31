# status-bot [![Circle CI](https://circleci.com/gh/fixme-lausanne/status-bot.svg?style=svg)](https://circleci.com/gh/fixme-lausanne/status-bot)

Check status of foo.fixme.ch from outside and inform us when something doesn't work.

## Build

You will need those dependencies to build and run the project

- [boot-clj](https://github.com/boot-clj/boot#install), a build tool and task runner.
- a JDK environment, I guess it should work with v1.6+ ? I only tested with v1.8.

```
# Clone the repo
git clone git@github.com:fixme-lausanne/status-bot.git

# Install dependencies (optional, missing dependencies are also
# fetched when running other tasks)
boot deps

# Build the project
boot build

# Run tests
boot run-tests

# You can see existing tasks
boot --help
```

## Usage

To manually run the bot:

```
export SLACK_API_TOKEN=XXX-XXXXXXXXXXXXX
java -jar target/status-bot-0.1.0-SNAPSHOT.jar
```

For a systemd unit, see the file [status-bot.service](https://github.com/fixme-lausanne/status-bot/blob/master/status-bot.service).
