<template>
  <div class="mt-6">
    <h1 class="title">Dashboard</h1>
    <div>
      <div>
        <h5 class="title is-5">
          <span class="tag is-rounded is-large is-dark">{{ throughput }}</span> device updates per second
        </h5>
      </div>
    </div>
    <div class="mt-6">
      <h5 class="title is-5">Trends</h5>
      <div class="table-container">        
        <table class="table is-striped is-hoverable">
          <tbody>
          <transition-group name="city-trends" tag="tbody">
            <tr v-for="item in cityTrendRanking" v-bind:key="item.city">
              <td>{{ item.city }}</td>
              <td>
                +{{ item.stepsCount }}
                <span class="has-text-weight-light">
                  ({{ item.moment.format("ddd	hh:mm:ss") }})
                </span>
              </td>
            </tr>
          </transition-group>
          </tbody>
        </table>
      </div>
    </div>
    <div class="mt-6">
      <h5 class="title is-5">Public ranking (last 24 hours)</h5>
      <div class="table-container">        
        <table class="table is-striped is-hoverable">
          <thead>
          <tr>
            <th>Name</th>
            <th>From</th>
            <th>Steps</th>
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

    const publicRankingStream = new EventSource("/dashboard/public-ranking");
    publicRankingStream.onmessage = (message) => {
      this.publicRanking = JSON.parse(message.data)
    }

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
