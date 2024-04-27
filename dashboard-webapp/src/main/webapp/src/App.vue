<template>
  <div>
    <div class="row mt-5">
      <div class="col">
        <h4>
          <span class="badge rounded-pill text-bg-dark">{{ throughput }}</span> device updates per second
        </h4>
      </div>
    </div>
    <div class="row mt-5">
      <div class="col">
        <h4>Trends</h4>
        <table class="table table-sm table-hover">
          <thead>
          <tr>
            <th scope="col">City</th>
            <th scope="col">Today's pulse</th>
          </tr>
          </thead>
          <transition-group name="city-trends" tag="tbody">
            <tr v-for="item in cityTrendRanking" v-bind:key="item.city">
              <td scope="row">{{ item.city }}</td>
              <td>
                +{{ item.stepsCount }}
                <span class="text-secondary font-weight-lighter">
                ({{ item.moment.format("ddd	hh:mm:ss") }})
                </span>
              </td>
            </tr>
          </transition-group>
        </table>
      </div>
    </div>
    <div class="row mt-5">
      <div class="col">
        <h4>Public ranking (last 24 hours)</h4>
        <table class="table table-sm table-hover">
          <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">From</th>
            <th scope="col">Steps</th>
          </tr>
          </thead>
          <transition-group name="public-ranking" tag="tbody">
            <tr v-for="item in publicRanking" v-bind:key="item.username">
              <td scope="row">{{ item.username }}</td>
              <td>{{ item.city }}</td>
              <td>{{ item.stepsCount }}</td>
            </tr>
          </transition-group>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.city-trends-move, .public-ranking-move {
  transition: transform 0.5s;
}
</style>

<script>
import moment from 'moment'

// import EventBus from 'vertx3-eventbus-client'
// import moment from 'moment'
//
// const eventBus = new EventBus("/eventbus")
// eventBus.enableReconnect(true)

export default {
  data() {
    return {
      throughput: 0,
      cityTrendData: {},
      publicRanking: []
    }
  },
  mounted() {

    const throughputStream = new EventSource("/dashboard/throughput");
    throughputStream.onmessage = (message) => {
      this.throughput = JSON.parse(message.data).throughput
    }

    const cityTrendStream = new EventSource("/dashboard/city-trends");
    cityTrendStream.onmessage = (message) => {
      const payload = JSON.parse(message.data)
      payload.moment = moment(payload.timestamp)
      this.cityTrendData[payload.city] = payload
    }

    // eventBus.onopen = () => {
    //   eventBus.registerHandler("client.updates.throughput", (err, message) => {
    //     this.throughput = message.body.throughput
    //   })
    //   eventBus.registerHandler("client.updates.city-trend", (err, message) => {
    //     const data = message.body
    //     data.moment = moment(data.timestamp)
    //     this.$set(this.cityTrendData, message.body.city, data)
    //   })
    //   eventBus.registerHandler("client.updates.publicRanking", (err, message) => {
    //     this.publicRanking = message.body
    //   })
    // }
  },
  computed: {
    cityTrendRanking: function () {
      const values = Object.values(this.cityTrendData).slice(0)
      values.sort((a, b) => b.stepsCount - a.stepsCount)
      return values
    }
  },
}
</script>
