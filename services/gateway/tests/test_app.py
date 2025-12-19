"""Basic tests for the gateway application scaffold."""

from gateway.app import create_app


def test_health_endpoint():
    app = create_app()
    routes = {route.path: route for route in app.routes}

    assert "/healthz" in routes
    health_route = routes["/healthz"]
    assert health_route.methods == {"GET"}
