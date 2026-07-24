uv init --bare
uv add locust
uv run locust -f ./locustfile.py --host http://localhost:3000
