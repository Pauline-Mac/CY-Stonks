import { VictoryChart, VictoryTheme, VictoryBar } from 'victory';

export default function Histogram() {
  return (
    <div>
      <VictoryChart
        theme={VictoryTheme.clean}
        domainPadding={{ x: 30 }}
      >
        <VictoryBar
          barRatio={0.8}
        />
      </VictoryChart>
    </div>
  );
}
