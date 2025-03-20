import { VictoryChart, VictoryTheme, VictoryBoxPlot, VictoryAxis } from 'victory';

interface BoxPlotProps {
  boxWidth: number;
  data: Array<BoxPlotData>;
  domainPadding?: number;
  horizontal?: boolean;
}

interface BoxPlotData {
  x: number;
  y: number[];
}

export default function BoxPlot({ boxWidth, data, horizontal, domainPadding }: BoxPlotProps) {
  return (
    <VictoryChart
      domainPadding={domainPadding}
      theme={VictoryTheme.clean}
      horizontal={horizontal}
    >
      <VictoryBoxPlot
        boxWidth={boxWidth}
        data={data}
      />
      <VictoryAxis
        dependentAxis
      //orientation="right"
      />
    </VictoryChart>
  );
}
