import React from 'react';

export default function CurrentTime() {
  const [current_time, setCurrentTime] = React.useState(new Date());

  React.useEffect(() => {
    const timerID = setInterval(() => tick(), 1000);

    return function cleanup() {
      clearInterval(timerID);
    };
  });

  function tick() {
    setCurrentTime(new Date());
  }

  return (
    <div className="current_time">
      {current_time.toLocaleTimeString()}
    </div>
  );
}
