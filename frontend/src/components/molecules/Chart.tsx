;
import { VictoryChart, VictoryAxis, VictoryTheme, VictoryGroup, VictoryLine, VictoryScatter, VictoryLegend, VictoryTooltip } from "victory";
import _ from "lodash";

export interface ChartProps {
  series: {
    x: number[] | string[],
    y: number[],
  }[],
  name: string[]
}

const symbols = [
  "circle",
  "diamond",
  "plus",
  "square",
  "triangleUp",
];

export default function Chart(props: ChartProps): React.ReactElement {
  const series = props.series.map((serie) => {
    const data = serie.y.slice(0, 7).map((value, i) => ({
      x: serie.x[i] || "",
      y: value,
    }));

    while (data.length < 7) {
      data.push({ x: "", y: 0 });
    }

    return {
      data,
      name: props.name[props.series.indexOf(serie)],
    };
  });

  const minValue = Math.min(...series.map((serie) => Math.min(...serie.data.map((d) => d.y))));
  const maxValue = Math.max(...series.map((serie) => Math.max(...serie.data.map((d) => d.y))));

  return (
    <VictoryChart

      padding={{
        top: 20,
        left: 70,
        right: 20,
        bottom: 70,
      }}
      theme={VictoryTheme.clean}
    >
      <VictoryAxis
        style={{
          tickLabels: {
            fontSize: 6,
            angle: 45,
            padding: 20,
          },
          ticks: {
            stroke: "#757575",
            size: 5,
          },
        }}
      />
      <VictoryAxis
        dependentAxis
        // label="Prix en euros"
        tickValues={_.range(minValue - 10, maxValue + 10, Math.ceil((maxValue - minValue) / 5))}
        tickFormat={(value) =>
          `${value} €`
        }
        style={{
          axis: {
            stroke: "transparent",
          },
          // axisLabel: {
          //   fontSize: 8,
          //   padding: 50,
          // },
          tickLabels: {
            fontSize: 8,
            padding: 20,
          },
        }}
      />
      {series.map((curreny_serie, i) => (
        <VictoryGroup
          data={curreny_serie.data}
          key={i}
          labelComponent={
            <VictoryTooltip
              style={{ fontSize: 10 }}
            />
          }
        >
          <VictoryLine
            style={{
              data: {
                stroke: VictoryTheme.clean.palette?.qualitative?.[i] || "blue",
                strokeWidth: 1,
              },
            }}
          />
          <VictoryScatter
            size={3}
            style={{
              data: {
                fill: VictoryTheme.clean.palette?.qualitative?.[i] || "blue",
              },
            }}
          />
        </VictoryGroup>
      ))}
      <VictoryLegend
        itemsPerRow={8}
        x={20}
        y={260}
        data={series.map((s, i) => ({
          name: s.name,
          symbol: {
            fill: VictoryTheme.clean.palette?.qualitative?.[i] || "blue",
            type: symbols[
              series.indexOf(s)
            ],
          },
        }))}
        style={{
          labels: {
            fontSize: 8,
          },
          border: {
            stroke: "transparent",
          },
        }}
      />
    </VictoryChart >
  );
}