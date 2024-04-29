<template>
  <div class="mt-6">    
    <div class="notification is-danger" role="alert" v-if="alertMessage.length > 0">
      {{ alertMessage }}
    </div>
    <h1 class="title">Registration</h1>
    <form v-on:submit.prevent="submit">
      <div class="field">
        <label class="label" for="username">User name</label>
        <input type="username" class="input" id="username" placeholder="somebody123" v-model="username">
      </div>
      <div class="field">
        <label class="label" for="email">Email</label>
        <input type="email" class="input" id="email" placeholder="foo@mail.me" v-model="email">
      </div>
      <div class="field">
        <label class="label" for="deviceId">Device identifier</label>
        <input type="deviceId" class="input" id="deviceId" placeholder="a1b2c3" v-model="deviceId">
      </div>
      <div class="field">
        <label class="label" for="password">Password</label>
        <input type="password" class="input" id="password" placeholder="abc123" v-model="password">
      </div>
      <div class="field">
        <label class="label" for="city">City</label>
        <input type="city" class="input" id="city" placeholder="Lyon" v-model="city">
      </div>
      <div class="field">
        <label class="checkbox" for="makePublic">
          <input type="checkbox" id="makePublic" v-model="makePublic">
          I want to appear in public rankings
        </label>
      </div>
      <div class="form-group">
        <button type="submit" class="button is-link">Submit</button>
      </div>
    </form>
  </div>
</template>

<script>
  import axios from 'axios'
  import Config from '../config'

  export default {
    data() {
      return {
        username: '',
        email: '',
        deviceId: '',
        city: '',
        password: '',
        makePublic: true,
        alertMessage: ''
      }
    },
    methods: {
      submit: function () {
        const payload = {
          username: this.username,
          email: this.email,
          deviceId: this.deviceId,
          password: this.password,
          city: this.city,
          makePublic: this.makePublic
        }
        axios
          .post(`${Config.userApiEndpoint}/register`, payload)
          .then(() => this.$router.push('/'))
          .catch(err => this.alertMessage = err.message)
      }
    }
  }
</script>
