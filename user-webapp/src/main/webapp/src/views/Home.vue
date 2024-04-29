<template>
  <div class="mt-6">
    <div class="notification is-danger" role="alert" v-if="alertMessage.length > 0">
      {{ alertMessage }}
    </div>
    <h1 class="title">Profile</h1>
    <div class="is-pulled-right">
      <button v-on:click="logout" class="button is-link is-danger" type="button">logout</button>
    </div>
    <div class="content">
      <ul>
        <li>Username: <span class="tag is-medium is-info">{{ username }}</span></li>
        <li>Device identifier: <span class="tag is-medium is-info">{{ deviceId }}</span></li>
        <li v-if="totalSteps > 0">
          You have done <span class="tag is-rounded is-medium is-primary">{{ totalSteps }}</span> steps in total,
          <span class="tag is-rounded is-medium is-primary">{{ stepsForMonth }}</span> this month, and
          <span class="tag is-rounded is-medium is-primary">{{ stepsForToday }}</span> today.
        </li>
        <li v-else>
          We don't have any stats for you yet.
        </li>
      </ul>
    </div>
    <div class="mt-6">
      <h2 class="subtitle">Update your details</h2>
      <form v-on:submit.prevent="sendUpdate">
        <div class="field">
          <label class="label" for="email">Email</label>
          <input type="email" class="input" id="email" placeholder="foo@mail.me" v-model="email">
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
  </div>
</template>

<script>
import DataStore from '../DataStore'
import Config from '../config'
import axios from 'axios'

export default {
  data() {
    return {
      username: 'n/a',
      city: 'n/a',
      email: 'n/a',
      deviceId: 'n/a',
      makePublic: false,
      totalSteps: 0,
      stepsForMonth: 0,
      stepsForToday: 0,
      alertMessage: ''
    }
  },
  mounted() {
    if (!DataStore.hasToken()) {
      this.$router.push({name: 'login'})
      return
    }
    this.refreshData()
    this.username = DataStore.username()
  },
  methods: {
    logout() {
      DataStore.reset()
      this.$router.push({name: 'login'})
    },
    refreshData() {
      axios
        .get(`${Config.userApiEndpoint}/${DataStore.username()}`, {
          headers: {
            'Authorization': `Bearer ${DataStore.token()}`
          }
        })
        .then(response => {
          DataStore.setCity(response.data.city)
          DataStore.setDeviceId(response.data.deviceId)
          DataStore.setEmail(response.data.email)
          DataStore.setMakePublic(response.data.makePublic)
          this.refreshFromDataStore()
        })
        .catch(err => this.alertMessage = err.message)

      const today = new Date()

      axios
        .get(`${Config.userApiEndpoint}/${DataStore.username()}/total`, {
          headers: {
            'Authorization': `Bearer ${DataStore.token()}`
          }
        })
        .then(response => this.totalSteps = response.data.count)
        .catch(err => {
          if (err.response.status === 404) {
            this.totalSteps = 0
          } else {
            this.alertMessage = err.message
          }
        })

      axios
        .get(`${Config.userApiEndpoint}/${DataStore.username()}/${today.getFullYear()}/${today.getUTCMonth() + 1}`, {
          headers: {
            'Authorization': `Bearer ${DataStore.token()}`
          }
        })
        .then(response => this.stepsForMonth = response.data.count)
        .catch(err => {
          if (err.response.status === 404) {
            this.stepsForMonth = 0
          } else {
            this.alertMessage = err.message
          }
        })

      axios
        .get(`${Config.userApiEndpoint}/${DataStore.username()}/${today.getFullYear()}/${today.getUTCMonth() + 1}/${today.getDate()}`, {
          headers: {
            'Authorization': `Bearer ${DataStore.token()}`
          }
        })
        .then(response => this.stepsForToday = response.data.count)
        .catch(err => {
          if (err.response.status === 404) {
            this.stepsForToday = 0
          } else {
            this.alertMessage = err.message
          }
        })
    },
    refreshFromDataStore() {
      this.city = DataStore.city()
      this.deviceId = DataStore.deviceId()
      this.email = DataStore.email()
      this.makePublic = DataStore.makePublic()
    },
    sendUpdate() {
      const data = {
        city: this.city,
        email: this.email,
        makePublic: this.makePublic
      }
      const config = {
        headers: {
          'Authorization': `Bearer ${DataStore.token()}`
        }
      }
      axios
        .put(`${Config.userApiEndpoint}/${DataStore.username()}`, data, config)
        .then(() => this.refreshData())
        .catch(err => {
          this.alertMessage = err.message
          this.refreshFromDataStore()
        })
    }
  },
}
</script>
