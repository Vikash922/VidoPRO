import requests
import uuid
from datetime import datetime
from typing import Any, Dict, List

BASE_URL = "http://localhost:8080"
TIMEOUT = 30.0
HEADERS = {"Content-Type": "application/json", "Accept": "application/json"}


def _parse_iso(ts: str) -> datetime:
    # Accept common ISO formats, including trailing Z
    if ts is None:
        raise ValueError("timestamp is None")
    if ts.endswith("Z"):
        ts = ts[:-1] + "+00:00"
    return datetime.fromisoformat(ts)


def test_post_create_project_with_name_and_aspect_ratio():
    created_project_id = None
    name = f"Test Project TC001 {uuid.uuid4()}"
    payload = {"name": name, "aspectRatio": "16:9"}

    try:
        # 1) Create project
        resp = requests.post(f"{BASE_URL}/projects", json=payload, headers=HEADERS, timeout=TIMEOUT)
        assert resp.status_code in (200, 201), f"Unexpected status code for create: {resp.status_code}, body: {resp.text}"
        project = resp.json()
        assert isinstance(project, dict), "Create response is not a JSON object"

        # Validate presence of core fields
        assert "id" in project and project["id"], "Created project missing 'id'"
        created_project_id = project["id"]

        assert project.get("name") == name, f"Project name mismatch: expected '{name}', got '{project.get('name')}'"
        assert "aspectRatio" in project, "Created project missing 'aspectRatio'"
        assert project["aspectRatio"] == payload["aspectRatio"], "Aspect ratio mismatch"

        # Validate timestamps and default metadata presence
        assert "createdAt" in project and project["createdAt"], "Missing createdAt"
        assert "updatedAt" in project and project["updatedAt"], "Missing updatedAt"

        created_at = _parse_iso(project["createdAt"])
        updated_at = _parse_iso(project["updatedAt"])
        assert updated_at >= created_at, "updatedAt is earlier than createdAt"

        # Optionally check for a metadata container (if present, at least ensure it's an object)
        if "metadata" in project:
            assert isinstance(project["metadata"], (dict, type(None))), "metadata should be an object or null"

        # 2) Get project list and verify presence and ordering by updatedAt desc
        list_resp = requests.get(f"{BASE_URL}/projects", headers=HEADERS, timeout=TIMEOUT)
        assert list_resp.status_code == 200, f"Unexpected status code for list: {list_resp.status_code}, body: {list_resp.text}"
        projects = list_resp.json()
        assert isinstance(projects, list), "Project list response is not an array"

        # Verify the created project is present in the list
        ids = [p.get("id") for p in projects]
        assert created_project_id in ids, "Created project ID not found in project list"

        # Verify the list is ordered by updatedAt descending
        parsed_updates: List[datetime] = []
        for p in projects:
            ua = p.get("updatedAt")
            assert ua is not None, f"Project in list missing updatedAt: {p}"
            parsed_updates.append(_parse_iso(ua))

        for earlier, later in zip(parsed_updates, parsed_updates[1:]):
            assert earlier >= later, "Project list is not ordered by updatedAt descending"

    except requests.RequestException as e:
        raise AssertionError(f"HTTP request failed: {e}")
    finally:
        # Cleanup: delete the created project if it exists
        if created_project_id:
            try:
                del_resp = requests.delete(f"{BASE_URL}/projects/{created_project_id}", headers=HEADERS, timeout=TIMEOUT)
                assert del_resp.status_code in (200, 202, 204), f"Unexpected status code for delete: {del_resp.status_code}, body: {del_resp.text}"
            except requests.RequestException as e:
                raise AssertionError(f"Failed to delete test project {created_project_id}: {e}")


if __name__ == "__main__":
    test_post_create_project_with_name_and_aspect_ratio()