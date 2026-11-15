import { Button } from '@/components/ui/button'

function App() {
  return (
    <main className="flex flex-col items-start gap-4 p-8">
      <h1 className="text-4xl font-bold">Bookit</h1>
      <div className="flex gap-2">
        <Button>Book now</Button>
        <Button variant="outline">Cancel</Button>
      </div>
    </main>
  )
}

export default App
