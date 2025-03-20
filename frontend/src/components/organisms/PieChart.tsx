import { VictoryPie, VictoryTheme } from 'victory';

const sampleData = [
  { x: "Cats", y: 35 },
  { x: "Dogs", y: 40 },
  { x: "Birds", y: 25 }
];

export default function PieChart() {
  return (
    <div>
      <h3>Click a pie slice below</h3>
      <VictoryPie
        events={[
          {
            target: "data",
            eventHandlers: {
              onClick: () => {
                return [
                  {
                    target: "data",
                    mutation: ({
                      style,
                    }) => {
                      return style.fill ===
                        "#c43a31"
                        ? null
                        : {
                          style: {
                            fill: "#c43a31",
                          },
                        };
                    },
                  },
                  {
                    target: "labels",
                    mutation: ({
                      text,
                    }) => {
                      return text ===
                        "clicked"
                        ? null
                        : {
                          text: "clicked",
                        };
                    },
                  },
                ];
              },
            },
          },
        ]}
        data={sampleData}
        theme={VictoryTheme.clean}
      />
    </div>
  );
}
