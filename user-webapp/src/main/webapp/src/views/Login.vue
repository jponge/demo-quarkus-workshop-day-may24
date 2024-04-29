<template>
  <div class="mt-6">    
    <div class="notification is-danger" role="alert" v-if="alertMessage.length > 0">
      {{ alertMessage }}
    </div>
    <h1 class="title">Login</h1>
    <form v-on:submit.prevent="login">
      <div class="field">
        <label class="label" for="username">User name</label>
        <input type="username" class="input" id="username" placeholder="somebody123" v-model="username">
      </div>
      <div class="field">
        <label class="label" for="password">Password</label>
        <input type="password" class="input" id="password" placeholder="abc123" v-model="password">
      </div>
      <button type="submit" class="button is-link">Submit</button>
    </form>
    <div>
      <p>...or <RouterLink to="/register">register</RouterLink></p>
    </div>
  </div>
</template>

<script>
  import DataStore from '../DataStore'
  import Config from '../config'
  import axios from 'axios'

  export default {
    data() {
      return {
        username: '',
        password: '',
        alertMessage: ''
      }
    },
    methods: {
      login: function () {
        if (this.username.length === 0 || this.password.length === 0) {
          return
        }
        axios
          .post(`${Config.userApiEndpoint}/token`, {
            username: this.username,
            password: this.password
          })
          .then(response => {
            DataStore.setToken(response.data)
            DataStore.setUsername(this.username)
            this.$router.push({name: 'home'})
          })
          .catch(err => this.alertMessage = err.message)
      }
    }
  }
</script>
