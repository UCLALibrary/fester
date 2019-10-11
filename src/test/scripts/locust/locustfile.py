from locust import HttpLocust, TaskSet
import uuid


# a very simple Locust task: upload a single testManifest.json file to the fester service

def upload(l):
    payload = {"filename": "testManifest.json"}
    l.client.put("/locust-test-{0}/manifest".format(uuid.uuid4().hex), json=payload)


class UserBehavior(TaskSet):
    tasks = {upload: 1}


class WebsiteUser(HttpLocust):
    task_set = UserBehavior
    min_wait = 5000
    max_wait = 9000
