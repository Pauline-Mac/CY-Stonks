export default function CurrentDate() {
  const date_string: string = new Date().toLocaleDateString('fr-FR', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  const formatted_date_string =
    date_string[0].toUpperCase() + date_string.substring(1);

  return (
    <div className="clock">
      <div className="current_date">{formatted_date_string}</div>
    </div>
  );
}
