const response = await fetch("/config/user-api-endpoint")

let userApiEndpoint;
if (response.ok) {
  userApiEndpoint = await response.text();
} else {
  userApiEndpoint = 'http://localhost:4000';
}

export default {
  userApiEndpoint
}
